@file:Suppress("UNCHECKED_CAST")

package com.fortysevendegrees.thool.spring.client

import arrow.core.Either
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.CombineParams
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.EndpointOutput
import com.fortysevendegrees.thool.Mapping
import com.fortysevendegrees.thool.Params
import com.fortysevendegrees.thool.PlainCodec
import com.fortysevendegrees.thool.RawBodyType
import com.fortysevendegrees.thool.SplitParams
import com.fortysevendegrees.thool.bodyType
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.StatusCode
import org.springframework.http.HttpHeaders
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestTemplate
import java.io.InputStream
import java.net.URI
import java.nio.ByteBuffer

public fun <I, E, O> Endpoint<I, E, O>.toRequestAndParseRestTemplate(
  baseUrl: String
): RestTemplate.(I) -> Pair<ClientHttpRequest, DecodeResult<Either<E, O>>> =
  { input: I ->
    val request = toRequest(requestFactory, baseUrl, input)
    Pair(request, parseResponse(request))
  }

public operator fun <I, E, O> RestTemplate.invoke(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): DecodeResult<Either<E, O>> {
  val request: ClientHttpRequest = endpoint.toRequest(requestFactory, baseUrl, input)
  return endpoint.parseResponse(request)
}

private fun <I, E, O> Endpoint<I, E, O>.toRequest(
  requestFactory: ClientHttpRequestFactory,
  baseUrl: String,
  i: I
): ClientHttpRequest {
  val params = Params.ParamsAsAny(i)
  val httpMethod = method()
  requireNotNull(httpMethod) { "Method not defined!" }
  val url = input.buildUrl(baseUrl.trimLastSlash(), params)
  return requestFactory.createRequest(URI.create(url), httpMethod).apply {
    input.setInputParams(this, params)
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
    is EndpointIO.StreamBody -> baseUrl
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
  request: ClientHttpRequest,
  params: Params
): ClientHttpRequest =
  (params.asAny as I).let { value ->
    when (val input = this) {
      is EndpointIO.Empty -> request
      is EndpointIO.Header -> input.codec.encode(value)
        .fold(request) { req, v -> req.apply { headers.add(input.name, v) } }

      is EndpointIO.Body<*, *> -> request.setBody(value, input.codec, input.bodyType)
      is EndpointInput.Cookie -> input.codec.encode(value)
        ?.let { v: String -> request.apply { headers.add(HttpHeaders.COOKIE, v) } }
        ?: request

      is EndpointIO.StreamBody -> TODO("Implement stream")

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

private fun <I> ClientHttpRequest.setBody(
  i: I,
  codec: Codec<*, *, CodecFormat>,
  rawBodyType: RawBodyType<*>
): ClientHttpRequest =
  when (rawBodyType) {
    RawBodyType.ByteArrayBody -> apply { body.write((codec::encode as (I) -> ByteArray)(i)) }
    RawBodyType.ByteBufferBody -> apply { body.write((codec::encode as (I) -> ByteBuffer)(i).array()) }
    RawBodyType.InputStreamBody -> apply { body.write((codec::encode as (I) -> InputStream)(i).readBytes()) }
    is RawBodyType.StringBody -> apply { body.write((codec::encode as (I) -> String)(i).toByteArray()) }
  }

private fun handleInputPair(
  left: EndpointInput<*>,
  right: EndpointInput<*>,
  params: Params,
  split: SplitParams,
  req: ClientHttpRequest
): ClientHttpRequest {
  val (leftParams, rightParams) = split(params)
  val req2 = (left as EndpointInput<Any?>).setInputParams(req, leftParams)
  return (right as EndpointInput<Any?>).setInputParams(req2, rightParams)
}

private fun handleMapped(
  tuple: EndpointInput<*>,
  codec: Mapping<*, *>,
  params: Params,
  req: ClientHttpRequest
): ClientHttpRequest =
  (tuple as EndpointInput<Any?>).setInputParams(
    req,
    Params.ParamsAsAny((codec::encode as (Any?) -> Any?)(params.asAny))
  )

// Functionality on how to go from Spring Response to our domain
private fun <I, E, O> Endpoint<I, E, O>.parseResponse(
  request: ClientHttpRequest,
): DecodeResult<Either<E, O>> {
  val response = request.execute()
  val code = StatusCode(response.rawStatusCode)
  val output = if (code.isSuccess()) output else errorOutput
  val parser = responseFromOutput(output)

  val headers = response.headers.toSingleValueMap()
    .mapNotNull { headerEntry: Map.Entry<String, String> -> Pair(headerEntry.key, headerEntry.value) }
    .groupBy({ it.first }) { it.second }
  val params =
    output.getOutputParams(parser(response), headers, code, response.statusCode.reasonPhrase)

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

private fun EndpointOutput<*>.getOutputParams(
  body: () -> Any?,
  headers: Map<String, List<String>>,
  code: StatusCode,
  statusText: String
): DecodeResult<Params> =
  when (val output = this) {
    is EndpointOutput.Single<*> -> when (val single = (output as EndpointOutput.Single<Any?>)) {
      is EndpointIO.Body<*, *> -> {
        single as EndpointIO.Body<Any?, Any?>
        val body = body.invoke()
        val decode: (Any?) -> DecodeResult<Any?> = (single.codec::decode)
        decode(body)
      }
      is EndpointIO.StreamBody -> TODO() // (output.codec::decode as (Any?) -> DecodeResult<Params>).invoke(body())
      is EndpointIO.Empty -> single.codec.decode(Unit)
      is EndpointOutput.FixedStatusCode -> single.codec.decode(Unit)
      is EndpointOutput.StatusCode -> single.codec.decode(code)
      is EndpointIO.Header -> single.codec.decode(headers[single.name].orEmpty())

      is EndpointIO.MappedPair<*, *, *, *> ->
        single.wrapped.getOutputParams(body, headers, code, statusText).flatMap { p ->
          (single.mapping::decode as (Any?) -> DecodeResult<Any?>)(p.asAny)
        }
      is EndpointOutput.MappedPair<*, *, *, *> ->
        single.output.getOutputParams(body, headers, code, statusText).flatMap { p ->
          (single.mapping::decode as (Any?) -> DecodeResult<Any?>)(p.asAny)
        }
    }.map { Params.ParamsAsAny(it) }

    is EndpointIO.Pair<*, *, *> -> handleOutputPair(
      output.first,
      output.second,
      output.combine,
      body,
      headers,
      code,
      statusText
    )
    is EndpointOutput.Pair<*, *, *> -> handleOutputPair(
      output.first,
      output.second,
      output.combine,
      body,
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
  body: () -> Any?,
  headers: Map<String, List<String>>,
  code: StatusCode,
  statusText: String
): DecodeResult<Params> {
  val l = left.getOutputParams(body, headers, code, statusText)
  val r = right.getOutputParams(body, headers, code, statusText)
  return l.flatMap { leftParams -> r.map { rightParams -> combine(leftParams, rightParams) } }
}

private fun responseFromOutput(output: EndpointOutput<*>): (ClientHttpResponse) -> () -> Any =
  { response: ClientHttpResponse ->
    {
      // TODO check if StreamBody
      when (output.bodyType()) {
        RawBodyType.ByteArrayBody -> response.body.readBytes()
        RawBodyType.ByteBufferBody -> response.body.bufferedReader()
        RawBodyType.InputStreamBody -> response.body.readBytes()
        is RawBodyType.StringBody -> String(response.body.readBytes())
        null -> Unit
      }
    }
  }
