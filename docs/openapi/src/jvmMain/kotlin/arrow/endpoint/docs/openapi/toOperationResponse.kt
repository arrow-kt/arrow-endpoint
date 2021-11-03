package arrow.endpoint.docs.openapi

import arrow.endpoint.Endpoint
import arrow.endpoint.EndpointIO
import arrow.endpoint.EndpointOutput
import arrow.endpoint.Schema
import arrow.endpoint.model.StatusCode

public fun Endpoint<*, *, *>.toOperationResponses(
  schemas: Map<Schema.ObjectInfo, String>
): Responses {
  // There always needs to be at least a 200 empty response
  return output.toResponse("200", Response(""), schemas) +
    errorOutput.toResponse("default", null, schemas)
}

public fun EndpointOutput<*>.toResponse(
  defaultResponseKey: String,
  defaultResponse: Response?,
  schemas: Map<Schema.ObjectInfo, String>
): Responses {
  val outputsList = asBasicOutputsList()
  val byStatusCode = outputsList.groupBy(Pair<StatusCode?, BasicOutputs>::first)

  val responses: Map<String, Referenced.Other<Response>> =
    outputsList
      .map { it.first }
      .distinct()
      .mapNotNull { code -> byStatusCode[code]?.let { Pair(code, it) } }
      .mapNotNull { (code, responses) ->
        val responseKey = code?.code?.toString() ?: defaultResponseKey
        responses
          .mapNotNull { (code, outputs) -> outputsToResponse(code, outputs, schemas) }
          .reduceOrNull(Response::plus)
          ?.let { response -> Pair(responseKey, Referenced.Other(response)) }
      }
      .toMap(linkedMapOf())

  val map: Map<String, Referenced.Other<Response>> =
    responses.ifEmpty {
      // no output at all - using default if defined
      defaultResponse?.let { Pair(defaultResponseKey, Referenced.Other(it)) }?.let {
        linkedMapOf(it)
      }
        ?: linkedMapOf()
    }

  return Responses(
    default = map["default"],
    responses =
      map.let { (it - "default") }.mapKeysTo(linkedMapOf()) { (k, _) -> StatusCode(k.toInt()) }
  )
}

private fun outputsToResponse(
  sc: StatusCode?,
  outputs: List<EndpointOutput<*>>,
  schemas: Map<Schema.ObjectInfo, String>
): Response? {
  val headers =
    outputs.mapNotNull {
      when (it) {
        is EndpointIO.Header ->
          Pair(
            it.name,
            Referenced.Other(
              Header(
                description = it.info.description,
                required = it.codec.schema().isNotOptional(),
                deprecated = it.info.deprecated,
                allowEmptyValue = null,
                explode = null,
                schema = schemas.referenceOr(it.codec),
                example = it.info.example()?.let { any -> ExampleValue(it.codec, any) },

                // TODO Fix showing Map<String, arrow.endpoint.docs.openapi.ExampleValue>
                examples = emptyMap() // it.info.examples.map { example -> .ExampleValue(it.codec,
                // example.value) }
                )
            )
          )
        else -> null
      }
    }

  val bodies =
    outputs.mapNotNull {
      when (it) {
        is EndpointIO.Body<*, *> ->
          Pair(it.info.description, it.codec.toMediaTypeMap(schemas, it.info.examples))
        else -> null
      }
    }

  val body = bodies.firstOrNull()

  val statusCodeDescriptions =
    outputs.flatMap {
      when (it) {
        is EndpointOutput.StatusCode ->
          it.documentedCodes.mapNotNull { (code, example) ->
            if (code == sc) example.description else null
          }
        is EndpointOutput.FixedStatusCode -> listOfNotNull(it.info.description)
        else -> emptyList()
      }
    }

  val description = body?.first ?: statusCodeDescriptions.firstOrNull() ?: ""

  val content = body?.second ?: linkedMapOf()

  return if (body != null || headers.isNotEmpty())
    Response(description, headers = headers.toMap(linkedMapOf()), content = content)
  else if (outputs.isNotEmpty()) Response(description) else null
}
