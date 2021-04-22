package com.fortysevendegrees.thool.http4k

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
import com.fortysevendegrees.thool.model.Method.Companion.GET
import com.fortysevendegrees.thool.model.Method.Companion.HEAD
import com.fortysevendegrees.thool.model.Method.Companion.POST
import com.fortysevendegrees.thool.model.Method.Companion.PUT
import com.fortysevendegrees.thool.model.Method.Companion.DELETE
import com.fortysevendegrees.thool.model.Method.Companion.OPTIONS
import com.fortysevendegrees.thool.model.Method.Companion.PATCH
import com.fortysevendegrees.thool.model.Method.Companion.CONNECT
import com.fortysevendegrees.thool.model.Method.Companion.TRACE
import com.fortysevendegrees.thool.model.StatusCode
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import java.io.InputStream
import java.nio.ByteBuffer

fun <I, E, O> Endpoint<I, E, O>.toRequestAndParser(baseUrl: String): (I) -> Pair<Request, (Response) -> DecodeResult<Either<E, O>>> =
  { input: I ->
    val request = toRequest(baseUrl, input)
    Pair(request, { response: Response -> parseResponse(request, response) })
  }

private fun String.trimLastSlash(): String =
  if (this.lastOrNull() == '/') dropLast(1) else this

operator fun <I, E, O> HttpHandler.invoke(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): DecodeResult<Either<E, O>> {
  val request = endpoint.toRequest(baseUrl, input)
  val response = invoke(request)
  return endpoint.parseResponse(request, response)
}

fun <I, E, O> Endpoint<I, E, O>.toRequest(
  baseUrl: String,
  i: I
): Request {
  val params = Params.ParamsAsAny(i)
  val request = Request(
    requireNotNull(method()) { "Method not defined!" },
    input.buildUrl(baseUrl.trimLastSlash(), params)
  )
  input.setInputParams(request, params)
  return request
}

fun EndpointInput<*>.buildUrl(
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

    // These don't influence baseUrl
    is EndpointIO.Body<*, *> -> baseUrl
    is EndpointIO.Empty -> baseUrl
    is EndpointInput.FixedMethod -> baseUrl
    is EndpointIO.Header -> baseUrl
    is EndpointIO.StreamBody -> baseUrl
    is EndpointInput.Query -> baseUrl
    is EndpointInput.Cookie -> baseUrl
    is EndpointInput.QueryParams -> baseUrl

    // Recurse on composition of inputs.
    is EndpointInput.Pair<*, *, *> -> handleInputPair(this.first, this.second, params, this.split, baseUrl)
    is EndpointIO.Pair<*, *, *> -> handleInputPair(this.first, this.second, params, this.split, baseUrl)
    is EndpointIO.MappedPair<*, *, *, *> -> handleMapped(this, this.mapping, params, baseUrl)
    is EndpointInput.MappedPair<*, *, *, *> -> handleMapped(this, this.mapping, params, baseUrl)
  }

fun handleInputPair(
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

// Extract method, and use GET as default
fun Endpoint<*, *, *>.method(): Method? =
  when (input.method()?.value ?: GET.value) {
    GET.value -> Method.GET
    HEAD.value -> Method.HEAD
    POST.value -> Method.POST
    PUT.value -> Method.PUT
    DELETE.value -> Method.DELETE
    OPTIONS.value -> Method.OPTIONS
    PATCH.value -> Method.PATCH
    TRACE.value -> Method.TRACE
    Method.PURGE.name -> Method.PURGE
    CONNECT.value -> null
    else -> null
  }

fun <I> EndpointInput<I>.setInputParams(
  request: Request,
  params: Params
): Request = (params.asAny as I).let { value ->
  when (val input = this) {
    is EndpointIO.Empty -> request
    is EndpointIO.Header ->
      input.codec.encode(value)
        .fold(request) { req, v -> req.header(input.name, v) }

    is EndpointIO.Body<*, *> -> request.setBody(value, input.codec, input.bodyType)
    is EndpointInput.Cookie -> input.codec.encode(value)?.let { v ->
      request.cookie(input.name, v)
    } ?: request

    is EndpointInput.Query ->
      input.codec.encode(value).fold(request) { request, v ->
        request.query(input.name, v)
      }

    is EndpointInput.QueryParams ->
      input.codec.encode(value).ps.fold(request) { request, (name, values) ->
        values.fold(request) { request, v ->
          request.query(name, v)
        }
      }

    is EndpointIO.StreamBody -> TODO("Implement stream")

    // These inputs were inserted into baseUrl already
    is EndpointInput.FixedMethod -> request
    is EndpointInput.FixedPath -> request
    is EndpointInput.PathCapture -> request
    is EndpointInput.PathsCapture -> request

    // Recurse on composition
    is EndpointIO.Pair<*, *, *> -> handleInputPair(input.first, input.second, params, input.split, request)
    is EndpointInput.Pair<*, *, *> -> handleInputPair(input.first, input.second, params, input.split, request)
    is EndpointIO.MappedPair<*, *, *, *> -> handleMapped(input, input.mapping, params, request)
    is EndpointInput.MappedPair<*, *, *, *> -> handleMapped(input, input.mapping, params, request)
  }
}

fun <I> Request.setBody(i: I, codec: Codec<*, *, CodecFormat>, rawBodyType: RawBodyType<*>): Request =
  when (rawBodyType) {
    RawBodyType.ByteArrayBody -> body(MemoryBody((codec::encode as (I) -> ByteArray)(i)))
    RawBodyType.ByteBufferBody -> body(Body((codec::encode as (I) -> ByteBuffer)(i)))
    RawBodyType.InputStreamBody -> body((codec::encode as (I) -> InputStream)(i))
    is RawBodyType.StringBody -> body((codec::encode as (I) -> String)(i))
  }

fun handleInputPair(
  left: EndpointInput<*>,
  right: EndpointInput<*>,
  params: Params,
  split: SplitParams,
  req: Request
): Request {
  val (leftParams, rightParams) = split(params)
  val req2 = (left as EndpointInput<Any?>).setInputParams(req, leftParams)
  return (right as EndpointInput<Any?>).setInputParams(req2, rightParams)
}

private fun handleMapped(
  tuple: EndpointInput<*>,
  codec: Mapping<*, *>,
  params: Params,
  req: Request
): Request =
  (tuple as EndpointInput<Any?>).setInputParams(
    req,
    Params.ParamsAsAny((codec::encode as (Any?) -> Any?)(params.asAny))
  )

// Functionality on how to go from Http4k Response to our domain
fun <I, E, O> Endpoint<I, E, O>.parseResponse(
  request: Request,
  response: Response
): DecodeResult<Either<E, O>> {
  val code = StatusCode(response.status.code)
  val output = if (code.isSuccess()) output else errorOutput
  val parser = responseFromOutput(output)

  val responseHeaders = response.headers
    .mapNotNull { if (it.second == null) null else it as Pair<String, String> }
    .groupBy({ it.first }) { it.second }

  val headers = response.cookies().asHeaders() + responseHeaders
  val params =
    output.getOutputParams(parser(response), headers, code, response.status.description)

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

fun List<org.http4k.core.cookie.Cookie>.asHeaders(): Map<String, List<String>> =
  mapOf("Set-Cookie" to map { c -> "${c.name}=${c.value}" })

fun EndpointOutput<*>.getOutputParams(
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
        val res = decode(body)
        res
      }
      is EndpointIO.StreamBody -> TODO("Support stream body")
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

private fun responseFromOutput(output: EndpointOutput<*>): (Response) -> () -> Any = { response: Response ->
  {
    // TODO check if StreamBody
    when (output.bodyType()) {
      RawBodyType.ByteArrayBody -> response.body.payload.array()
      RawBodyType.ByteBufferBody -> response.body.payload
      RawBodyType.InputStreamBody -> response.body.stream
      is RawBodyType.StringBody -> response.body.toString()
      null -> Unit
    }
  }
}
