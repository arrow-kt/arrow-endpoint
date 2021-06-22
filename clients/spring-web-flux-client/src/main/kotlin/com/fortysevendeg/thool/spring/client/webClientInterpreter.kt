package com.fortysevendeg.thool.spring.client

import arrow.core.Either
import com.fortysevendeg.thool.CombineParams
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.EndpointOutput
import com.fortysevendeg.thool.Mapping
import com.fortysevendeg.thool.Params
import com.fortysevendeg.thool.client.RequestInfo
import com.fortysevendeg.thool.client.requestInfo
import com.fortysevendeg.thool.model.Method
import com.fortysevendeg.thool.model.StatusCode
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import java.net.URI
import java.nio.ByteBuffer
import reactor.core.publisher.Mono

import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import java.io.ByteArrayInputStream

public suspend operator fun <I, E, O> WebClient.invoke(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): DecodeResult<Either<E, O>> {
  val info = endpoint.input.requestInfo(input, baseUrl)
  val method = info.method.method()
  requireNotNull(method)
  val request = toRequest(info, method)
  return request.awaitExchange { response ->
    endpoint.parseResponse(method, baseUrl, response)
  }
}

public suspend fun <I, E, O> WebClient.execute(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): Triple<WebClient.RequestBodyUriSpec, ClientResponse, DecodeResult<Either<E, O>>> {
  val info = endpoint.input.requestInfo(input, baseUrl)
  val method = info.method.method()
  requireNotNull(method)
  val request = toRequest(info, method)
  return request.awaitExchange { response ->
    Triple(request, response, endpoint.parseResponse(method, baseUrl, response))
  }
}

public fun WebClient.toRequest(
  info: RequestInfo,
  method: HttpMethod
): WebClient.RequestBodyUriSpec =
  method(method).apply {
    uri(URI.create(info.fullUrl))
    info.headers.forEach { (name, value) ->
      header(name, value)
    }
    info.cookies.forEach { (name, value) ->
      cookie(name, value)
    }
    info.body?.toByteArray()?.let {
      body(BodyInserters.fromPublisher(Mono.just(it), ByteArray::class.java))
    }
  }

public suspend fun <I, E, O> Endpoint<I, E, O>.parseResponse(
  method: HttpMethod,
  url: String,
  response: ClientResponse
): DecodeResult<Either<E, O>> {
  val code = StatusCode(response.rawStatusCode())
  val output = if (code.isSuccess()) output else errorOutput

  val params =
    output.getOutputParams(response, response.headers().asHttpHeaders(), code, response.statusCode().reasonPhrase)

  @Suppress("UNCHECKED_CAST")
  val result = params.map { it.asAny }
    .map { p -> if (code.isSuccess()) Either.Right(p as O) else Either.Left(p as E) }

  return when (result) {
    is DecodeResult.Failure.Error ->
      DecodeResult.Failure.Error(
        result.original,
        IllegalArgumentException(
          "Cannot decode from ${result.original} of request ${method.name} $url",
          result.error
        )
      )
    else -> result
  }
}

public fun Method.method(): HttpMethod? =
  when (this.value) {
    Method.GET.value -> HttpMethod.GET
    Method.HEAD.value -> HttpMethod.HEAD
    Method.POST.value -> HttpMethod.POST
    Method.PUT.value -> HttpMethod.PUT
    Method.DELETE.value -> HttpMethod.DELETE
    Method.OPTIONS.value -> HttpMethod.OPTIONS
    Method.PATCH.value -> HttpMethod.PATCH
    Method.TRACE.value -> HttpMethod.TRACE
    Method.CONNECT.value -> null
    else -> null
  }

@Suppress("UNCHECKED_CAST")
private suspend fun EndpointOutput<*>.getOutputParams(
  response: ClientResponse,
  headers: Map<String, List<String>>,
  code: StatusCode,
  statusText: String
): DecodeResult<Params> =
  when (val output = this) {
    is EndpointOutput.Single<*> -> when (val single = (output as EndpointOutput.Single<Any?>)) {
      is EndpointIO.ByteArrayBody -> single.codec.decode(response.awaitBodyOrNull(ByteArray::class) ?: byteArrayOf())
      is EndpointIO.ByteBufferBody -> single.codec.decode(
        response.awaitBodyOrNull(ByteBuffer::class) ?: ByteBuffer.wrap(byteArrayOf())
      )
      is EndpointIO.InputStreamBody -> single.codec.decode(
        ByteArrayInputStream(
          response.awaitBodyOrNull(ByteArray::class) ?: byteArrayOf()
        )
      )
      is EndpointIO.StringBody -> single.codec.decode(response.awaitBodyOrNull(String::class) ?: "")
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

private suspend fun handleOutputPair(
  left: EndpointOutput<*>,
  right: EndpointOutput<*>,
  combine: CombineParams,
  response: ClientResponse,
  headers: Map<String, List<String>>,
  code: StatusCode,
  statusText: String
): DecodeResult<Params> {
  val l = left.getOutputParams(response, headers, code, statusText)
  val r = right.getOutputParams(response, headers, code, statusText)
  return l.flatMap { leftParams -> r.map { rightParams -> combine(leftParams, rightParams) } }
}
