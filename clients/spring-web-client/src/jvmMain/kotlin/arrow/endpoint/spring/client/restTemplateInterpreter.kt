package arrow.endpoint.spring.client

import arrow.core.Either
import arrow.endpoint.CombineParams
import arrow.endpoint.DecodeResult
import arrow.endpoint.Endpoint
import arrow.endpoint.EndpointIO
import arrow.endpoint.EndpointOutput
import arrow.endpoint.Mapping
import arrow.endpoint.Params
import arrow.endpoint.client.requestInfo
import arrow.endpoint.model.Method
import arrow.endpoint.model.StatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.nio.ByteBuffer

public fun <A> DecodeResult<A>.getOrThrow(): A =
  when (this) {
    is DecodeResult.Value -> this.value
    is DecodeResult.Failure.Error -> throw this.error
    else -> throw IllegalArgumentException("Cannot decode: $this")
  }

public fun <A> DecodeResult<A>.getOrNull(): A? =
  when (this) {
    is DecodeResult.Value -> this.value
    else -> null
  }

public suspend operator fun <I, E, O> RestTemplate.invoke(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): DecodeResult<Either<E, O>> {
  val request = endpoint.toRequest(requestFactory, baseUrl, input)
  val response = runInterruptible(Dispatchers.IO) { request.execute() }
  return endpoint.parseResponse(request, response)
}

public suspend fun <I, E, O> RestTemplate.execute(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): Triple<ClientHttpRequest, ClientHttpResponse, DecodeResult<Either<E, O>>> {
  val request = endpoint.toRequest(requestFactory, baseUrl, input)
  val response = runInterruptible(Dispatchers.IO) { request.execute() }
  val result = endpoint.parseResponse(request, response)
  return Triple(request, response, result)
}

public fun <I, E, O> Endpoint<I, E, O>.toRequest(
  requestFactory: ClientHttpRequestFactory,
  baseUrl: String,
  i: I
): ClientHttpRequest {
  val info = input.requestInfo(i, baseUrl)
  val httpMethod = info.method.method()
  requireNotNull(httpMethod) { "Method not defined!" }
  return requestFactory.createRequest(URI.create(info.fullUrl), httpMethod).apply {
    info.cookies.forEach { (name, value) ->
      headers.add(name, value)
    }
    info.headers.forEach { (name, value) ->
      headers.add(name, value)
    }
    info.body?.toByteArray()?.let(body::write)
  }
}

public fun <I, E, O> Endpoint<I, E, O>.parseResponse(
  request: ClientHttpRequest,
  response: ClientHttpResponse
): DecodeResult<Either<E, O>> {
  val code = StatusCode(response.rawStatusCode)
  val output = if (code.isSuccess()) output else errorOutput

  val params =
    output.getOutputParams(response, response.headers, code, response.statusCode.reasonPhrase)

  @Suppress("UNCHECKED_CAST")
  val result = params.map { it.asAny }
    .map { p -> if (code.isSuccess()) Either.Right(p as O) else Either.Left(p as E) }

  return when (result) {
    is DecodeResult.Failure.Error ->
      DecodeResult.Failure.Error(
        result.original,
        IllegalArgumentException(
          "Cannot decode from ${result.original} of request ${request.method} ${request.uri}",
          result.error
        )
      )
    else -> result
  }
}

public fun Method.method(): HttpMethod? =
  when (this) {
    Method.GET -> HttpMethod.GET
    Method.HEAD -> HttpMethod.HEAD
    Method.POST -> HttpMethod.POST
    Method.PUT -> HttpMethod.PUT
    Method.DELETE -> HttpMethod.DELETE
    Method.OPTIONS -> HttpMethod.OPTIONS
    Method.PATCH -> HttpMethod.PATCH
    Method.TRACE -> HttpMethod.TRACE
    Method.CONNECT -> null
    else -> null
  }

@Suppress("UNCHECKED_CAST")
private fun EndpointOutput<*>.getOutputParams(
  response: ClientHttpResponse,
  headers: Map<String, List<String>>,
  code: StatusCode,
  statusText: String
): DecodeResult<Params> =
  when (val output = this) {
    is EndpointOutput.Single<*> -> when (val single = (output as EndpointOutput.Single<Any?>)) {
      is EndpointIO.ByteArrayBody -> single.codec.decode(response.body.readBytes())
      is EndpointIO.ByteBufferBody -> single.codec.decode(ByteBuffer.wrap(response.body.readBytes()))
      is EndpointIO.InputStreamBody -> single.codec.decode(response.body.readBytes().inputStream())
      is EndpointIO.StringBody -> single.codec.decode(String(response.body.readBytes()))
      is EndpointIO.Empty -> single.codec.decode(Unit)
      is EndpointOutput.FixedStatusCode -> single.codec.decode(Unit)
      is EndpointOutput.StatusCode -> single.codec.decode(code)
      is EndpointIO.Header -> single.codec.decode(headers[single.name].orEmpty())
      is EndpointOutput.OneOf<*, *> -> single.mappings.firstOrNull { it.statusCode == null || it.statusCode == code }
        ?.let { mapping -> mapping.output.getOutputParams(response, headers, code, statusText).flatMap { p -> (single.codec as Mapping<Any?, Any?>).decode(p.asAny) } }
        ?: DecodeResult.Failure.Error(statusText, IllegalArgumentException("Cannot find mapping for status code $code in outputs $output"))

      is EndpointIO.MappedPair<*, *, *, *> ->
        single.wrapped.getOutputParams(response, headers, code, statusText).flatMap { p ->
          (single.mapping as Mapping<Any?, Any?>).decode(p.asAny)
        }
      is EndpointOutput.MappedPair<*, *, *, *> ->
        single.output.getOutputParams(response, headers, code, statusText).flatMap { p ->
          (single.mapping as Mapping<Any?, Any?>).decode(p.asAny)
        }
    }.map { Params.ParamsAsAny(it) }

    is EndpointIO.Pair<*, *, *> -> handleOutputPair(
      output.first,
      output.second,
      output.combine,
      response,
      headers,
      code,
      statusText
    )
    is EndpointOutput.Pair<*, *, *> -> handleOutputPair(
      output.first,
      output.second,
      output.combine,
      response,
      headers,
      code,
      statusText
    )
    is EndpointOutput.Void -> DecodeResult.Failure.Error(
      "",
      IllegalArgumentException("Cannot convert a void output to a value!")
    )
  }

private fun handleOutputPair(
  left: EndpointOutput<*>,
  right: EndpointOutput<*>,
  combine: CombineParams,
  response: ClientHttpResponse,
  headers: Map<String, List<String>>,
  code: StatusCode,
  statusText: String
): DecodeResult<Params> {
  val l = left.getOutputParams(response, headers, code, statusText)
  val r = right.getOutputParams(response, headers, code, statusText)
  return l.flatMap { leftParams -> r.map { rightParams -> combine(leftParams, rightParams) } }
}
