@file:Suppress("UNCHECKED_CAST")

package com.fortysevendeg.thool.spring.client

import arrow.core.Either
import com.fortysevendeg.thool.Codec
import com.fortysevendeg.thool.CombineParams
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.EndpointInput
import com.fortysevendeg.thool.EndpointOutput
import com.fortysevendeg.thool.Mapping
import com.fortysevendeg.thool.Params
import com.fortysevendeg.thool.PlainCodec
import com.fortysevendeg.thool.SplitParams
import com.fortysevendeg.thool.client.RequestInfo
import com.fortysevendeg.thool.client.requestInfo
import com.fortysevendeg.thool.model.CodecFormat
import com.fortysevendeg.thool.model.StatusCode
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import java.io.InputStream
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
    Pair(request, parseResponse(request, method, baseUrl))
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
  return endpoint.parseResponse(request, method, baseUrl)
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

private fun EndpointInput<*>.buildUrl(
  baseUrl: String,
  params: Params
): String =
  when (this) {
    is EndpointInput.FixedPath -> "$baseUrl/${this.s}"
    is EndpointInput.PathCapture -> {
      val v = (codec as PlainCodec<Any?>).encode(params.asAny)
      "$baseUrl/$v"
    }
    is EndpointInput.PathsCapture -> {
      val ps = (codec as Codec<List<String>, Any?, CodecFormat.TextPlain>).encode(params.asAny)
      baseUrl + ps.joinToString(prefix = "/", separator = "/")
    }
    is EndpointInput.Query -> {
      val ps = (codec as Codec<List<String>, Any?, CodecFormat.TextPlain>).encode(params.asAny)
      baseUrl + ps.joinToString(prefix = "?", separator = "/")
    }
    is EndpointInput.QueryParams -> {
      val ps = (codec as Codec<List<Pair<String, List<String>>>, Any?, CodecFormat.TextPlain>).encode(params.asAny)
      baseUrl + ps.joinToString(prefix = "?", separator = "&")
    }

    // These don't influence baseUrl
    is EndpointIO.Body<*, *> -> baseUrl
    is EndpointIO.Empty -> baseUrl
    is EndpointInput.FixedMethod -> baseUrl
    is EndpointIO.Header -> baseUrl
    is EndpointInput.Cookie -> baseUrl

    // Recurse on composition of inputs.
    is EndpointInput.Pair<*, *, *> -> handleInputPair(this.first, this.second, params, this.split, baseUrl)
    is EndpointIO.Pair<*, *, *> -> handleInputPair(this.first, this.second, params, this.split, baseUrl)
    is EndpointIO.MappedPair<*, *, *, *> -> handleMapped(this, this.mapping, params, baseUrl)
    is EndpointInput.MappedPair<*, *, *, *> -> handleMapped(this, this.mapping, params, baseUrl)
  }

private fun handleInputPair(
  left: EndpointInput<*>,
  right: EndpointInput<*>,
  params: Params,
  split: SplitParams,
  baseUrl: String
): String {
  val (leftParams, rightParams) = split(params)
  val baseUrl2 = (left as EndpointInput<Any?>).buildUrl(baseUrl, leftParams)
  return (right as EndpointInput<Any?>).buildUrl(baseUrl2, rightParams)
}

private fun handleMapped(
  tuple: EndpointInput<*>,
  codec: Mapping<*, *>,
  params: Params,
  baseUrl: String
): String =
  (tuple as EndpointInput<Any?>).buildUrl(
    baseUrl,
    Params.ParamsAsAny((codec::encode as (Any?) -> Any?)(params.asAny))
  )

private fun <I> EndpointInput<I>.setInputParams(
  request: WebClient.RequestBodyUriSpec,
  params: Params
): WebClient.RequestBodyUriSpec =
  (params.asAny as I).let { value ->
    when (val input = this) {
      is EndpointIO.Empty -> request
      is EndpointIO.Header -> input.codec.encode(value).fold(request) { req, v -> req.apply { header(input.name, v) } }
      is EndpointIO.ByteArrayBody -> request.apply { body((input.codec::encode)(value), ByteArray::class.java) }
      is EndpointIO.ByteBufferBody -> request.apply { body((input.codec::encode)(value), ByteBuffer::class.java) }
      is EndpointIO.InputStreamBody -> request.apply { body((input.codec::encode)(value), InputStream::class.java) }
      is EndpointIO.StringBody -> request.apply { body((input.codec::encode)(value), String::class.java) }
      is EndpointInput.Cookie -> input.codec.encode(value)?.let { v: String -> request.apply { cookie(input.name, v) } }
        ?: request

      // These inputs were inserted into baseUrl already
      is EndpointInput.FixedMethod -> request
      is EndpointInput.FixedPath -> request
      is EndpointInput.PathCapture -> request
      is EndpointInput.PathsCapture -> request
      is EndpointInput.Query -> request
      is EndpointInput.QueryParams -> request

      // Recurse on composition
      is EndpointIO.Pair<*, *, *> -> handleInputPair(input.first, input.second, params, input.split, request)
      is EndpointInput.Pair<*, *, *> -> handleInputPair(input.first, input.second, params, input.split, request)
      is EndpointIO.MappedPair<*, *, *, *> -> handleMapped(input, input.mapping, params, request)
      is EndpointInput.MappedPair<*, *, *, *> -> handleMapped(input, input.mapping, params, request)
    }
  }

private fun handleInputPair(
  left: EndpointInput<*>,
  right: EndpointInput<*>,
  params: Params,
  split: SplitParams,
  req: WebClient.RequestBodyUriSpec
): WebClient.RequestBodyUriSpec {
  val (leftParams, rightParams) = split(params)
  val req2 = (left as EndpointInput<Any?>).setInputParams(req, leftParams)
  return (right as EndpointInput<Any?>).setInputParams(req2, rightParams)
}

private fun handleMapped(
  tuple: EndpointInput<*>,
  codec: Mapping<*, *>,
  params: Params,
  req: WebClient.RequestBodyUriSpec
): WebClient.RequestBodyUriSpec =
  (tuple as EndpointInput<Any?>)
    .setInputParams(req, Params.ParamsAsAny((codec::encode as (Any?) -> Any?)(params.asAny)))

// Functionality on how to go from Spring response to our domain
private suspend fun <I, E, O> Endpoint<I, E, O>.parseResponse(
  request: WebClient.RequestBodyUriSpec,
  method: HttpMethod,
  url: String
): DecodeResult<Either<E, O>> =
  request.awaitExchange { response: ClientResponse ->
    val code = StatusCode(response.rawStatusCode())
    val output = if (code.isSuccess()) output else errorOutput

    val params =
      output.getOutputParams(response, response.headers().asHttpHeaders(), code, response.statusCode().reasonPhrase)

    params.map { it.asAny }
      .map { p -> if (code.isSuccess()) Either.Right(p as O) else Either.Left(p as E) }
  }.let { result: DecodeResult<Either<E, O>> ->
    when (result) {
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