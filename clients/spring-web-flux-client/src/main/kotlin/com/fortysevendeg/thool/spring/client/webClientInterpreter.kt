@file:Suppress("UNCHECKED_CAST")

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

public fun <I, E, O> Endpoint<I, E, O>.toRequestAndParseWebClient(
  baseUrl: String
): suspend WebClient.(I) -> Pair<WebClient.RequestBodyUriSpec, DecodeResult<Either<E, O>>> =
  { input: I ->
    val info = this@toRequestAndParseWebClient.input.requestInfo(input, baseUrl)
    val method = info.method.method()
    requireNotNull(method)
    val request: WebClient.RequestBodyUriSpec = toRequest(this, info, method)
    request.awaitExchange { response ->
      Pair(request, parseResponse(request, method, baseUrl, response))
    }
  }

public suspend fun <I, E, O> WebClient.invokeAndResponse(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): Pair<DecodeResult<Either<E, O>>, ClientResponse> {
  val info = endpoint.input.requestInfo(input, baseUrl)
  val method = info.method.method()
  requireNotNull(method)
  val request: WebClient.RequestBodyUriSpec = toRequest(this, info, method)
  return request.awaitExchange { response ->
    Pair(endpoint.parseResponse(request, method, baseUrl, response), response)
  }
}

public suspend operator fun <I, E, O> WebClient.invoke(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): DecodeResult<Either<E, O>> {
  val info = endpoint.input.requestInfo(input, baseUrl)
  val method = info.method.method()
  requireNotNull(method)
  val request: WebClient.RequestBodyUriSpec = toRequest(this, info, method)
  return request.awaitExchange { endpoint.parseResponse(request, method, baseUrl, it) }
}

private fun toRequest(
  webClient: WebClient,
  info: RequestInfo,
  method: HttpMethod,
): WebClient.RequestBodyUriSpec {
  return webClient.method(method).apply {
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
}

// Functionality on how to go from Spring response to our domain
private suspend fun <I, E, O> Endpoint<I, E, O>.parseResponse(
  request: WebClient.RequestBodyUriSpec,
  method: HttpMethod,
  url: String,
  response: ClientResponse
): DecodeResult<Either<E, O>> {
  val code = StatusCode(response.rawStatusCode())
  val output = if (code.isSuccess()) output else errorOutput

  val params =
    output.getOutputParams(response, response.headers().asHttpHeaders(), code, response.statusCode().reasonPhrase)

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
