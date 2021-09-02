package com.fortysevendeg.thool.docs.openapi

import arrow.core.Either
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.EndpointInput
import com.fortysevendeg.thool.Schema
import com.fortysevendeg.thool.model.Method

internal class PathCreator(
  private val schemas: Map<Schema.ObjectInfo, String>,
  private val securitySchemes: SecuritySchemes,
  private val options: OpenAPIDocsOptions
) {

  fun pathItem(e: Endpoint<*, *, *>): Pair<String, PathItem> {
    val inputs = e.input.asListOfBasicInputs(includeAuth = false)
    val pathComponents = namedPathComponents(inputs)
    val method = e.input.method() ?: Method.GET

    val defaultId = options.operationIdGenerator(pathComponents, method)

    val operation = endpointToOperation(defaultId, e, inputs)
    val pathItem = PathItem(
      summary = null,
      description = null,
      get = if (method == Method.GET) operation else null,
      put = if (method == Method.PUT) operation else null,
      post = if (method == Method.POST) operation else null,
      delete = if (method == Method.DELETE) operation else null,
      options = if (method == Method.OPTIONS) operation else null,
      head = if (method == Method.HEAD) operation else null,
      patch = if (method == Method.PATCH) operation else null,
      trace = if (method == Method.TRACE) operation else null,
      servers = emptyList(),
      parameters = emptyList()
    )

    return Pair(e.renderPath(renderQueryParam = null, includeAuth = false), pathItem)
  }

  private fun operationSecurity(e: Endpoint<*, *, *>): List<SecurityRequirement> {
    val securityRequirement: SecurityRequirement = e.input.auths().mapNotNull { auth ->
      securitySchemes[auth]?.first?.let { Pair(it, emptyList<String>()) }
    }.toMap()

    return when {
      securityRequirement.isEmpty() -> emptyList()
      else -> {
        val securityOptional =
          e.input.auths()
            .flatMap { it.asListOfBasicInputs() }
            .all { it.codec.schema().isOptional() }

        if (securityOptional) listOf(emptyMap(), securityRequirement)
        else listOf(securityRequirement)
      }
    }
  }

  private fun endpointToOperation(defaultId: String, e: Endpoint<*, *, *>, inputs: List<EndpointInput.Basic<*, *, *>>): Operation {
    val parameters: List<Parameter> = operationParameters(inputs)
    val body: List<Referenced<RequestBody>> = operationInputBody(inputs)
    val responses = e.toOperationResponses(schemas)

    return Operation(
      tags = e.info.tags,
      summary = e.info.summary,
      description = e.info.description,
      operationId = e.info.name ?: defaultId,
      parameters = parameters.map { Referenced.Other(it) },
      requestBody = body.firstOrNull(),
      responses = responses,
      deprecated = e.info.deprecated,
      security = operationSecurity(e),
      servers = emptyList()
    )
  }

  private fun EndpointIO.Body<*, *>.toRequestBody(): RequestBody =
    RequestBody(info.description, codec.toMediaTypeMap(schemas, info.examples), codec.schema().isNotOptional())

  private fun operationInputBody(inputs: List<EndpointInput.Basic<*, *, *>>): List<Referenced<RequestBody>> =
    inputs.mapNotNull {
      when (it) {
        is EndpointIO.Body<*, *> -> Referenced.Other(it.toRequestBody())
        else -> null
      }
    }

  private fun operationParameters(inputs: List<EndpointInput.Basic<*, *, *>>) =
    inputs.mapNotNull {
      when (it) {
        is EndpointInput.Query -> queryToParameter(it)
        is EndpointInput.PathCapture -> pathCaptureToParameter(it)
        is EndpointIO.Header -> headerToParameter(it)
        is EndpointInput.Cookie -> cookieToParameter(it)
//        is EndpointIO . FixedHeader [_]    => fixedHeaderToParameter(f)
        else -> null
      }
    }

  private fun <A> headerToParameter(header: EndpointIO.Header<A>) =
    header.toParameter(schemas.referenceOr(header.codec))

  private fun <A> cookieToParameter(cookie: EndpointInput.Cookie<A>) =
    cookie.toParameter(schemas.referenceOr(cookie.codec))

  private fun <A> pathCaptureToParameter(p: EndpointInput.PathCapture<A>) =
    p.toParameter(schemas.referenceOr(p.codec))

  private fun <A> queryToParameter(query: EndpointInput.Query<A>) =
    query.toParameter(schemas.referenceOr(query.codec))

  private fun namedPathComponents(inputs: List<EndpointInput.Basic<*, *, *>>): List<String> =
    inputs.mapNotNull {
      when (it) {
        is EndpointInput.PathCapture -> Either.Left(it.name)
        is EndpointInput.FixedPath -> Either.Right(it.s)
        else -> null
      }
    }.fold(emptyList()) { acc, component ->
      when (component) {
        is Either.Left ->
          component.value?.let { acc + it } ?: throw IllegalStateException("All path captures should be named")
        is Either.Right -> acc + component.value
      }
    }
}
