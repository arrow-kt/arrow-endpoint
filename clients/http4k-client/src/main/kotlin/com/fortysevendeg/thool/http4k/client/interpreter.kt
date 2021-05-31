package com.fortysevendeg.thool.http4k

import arrow.core.Either
import com.fortysevendeg.thool.CombineParams
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.EndpointOutput
import com.fortysevendeg.thool.Mapping
import com.fortysevendeg.thool.Params
import com.fortysevendeg.thool.client.requestInfo
import com.fortysevendeg.thool.model.Body
import com.fortysevendeg.thool.model.Method.Companion.GET
import com.fortysevendeg.thool.model.Method.Companion.HEAD
import com.fortysevendeg.thool.model.Method.Companion.POST
import com.fortysevendeg.thool.model.Method.Companion.PUT
import com.fortysevendeg.thool.model.Method.Companion.DELETE
import com.fortysevendeg.thool.model.Method.Companion.OPTIONS
import com.fortysevendeg.thool.model.Method.Companion.PATCH
import com.fortysevendeg.thool.model.Method.Companion.CONNECT
import com.fortysevendeg.thool.model.Method.Companion.TRACE
import com.fortysevendeg.thool.model.StatusCode
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies

public fun <I, E, O> Endpoint<I, E, O>.toRequestAndParser(baseUrl: String): (I) -> Pair<Request, (Response) -> DecodeResult<Either<E, O>>> =
  { input: I ->
    val request = toRequest(baseUrl, input)
    Pair(request, { response: Response -> parseResponse(request, response) })
  }

public operator fun <I, E, O> HttpHandler.invoke(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): DecodeResult<Either<E, O>> {
  val request = endpoint.toRequest(baseUrl, input)
  val response = invoke(request)
  return endpoint.parseResponse(request, response)
}

public fun <I, E, O> Endpoint<I, E, O>.toRequest(baseUrl: String, i: I): Request {
  val info = input.requestInfo(i, baseUrl)
  val r = Request(
    requireNotNull(info.method.toHttp4kMethod()) { "Method ${info.method.value} not supported!" },
    info.baseUrlWithPath
  )
  val r2 = info.cookies.fold(r) { r, (name, value) ->
    r.cookie(name, value)
  }
  val r3 = info.headers.fold(r2) { r, (name, value) ->
    r.header(name, value)
  }
  val r4 = info.queryParams.ps.fold(r3) { r, (name, params) ->
    params.fold(r) { r, v ->
      r.query(name, v)
    }
  }

  return when (val body = info.body) {
    is Body.ByteArray -> r4.body(MemoryBody(body.byteArray))
    is Body.ByteBuffer -> r4.body(MemoryBody(body.byteBuffer))
    is Body.InputStream -> r4.body(body.inputStream)
    is Body.String -> r4.body(body.string)
    null -> r4
  }
}

public fun com.fortysevendeg.thool.model.Method.toHttp4kMethod(): Method? =
  when (this.value) {
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

// Functionality on how to go from Http4k Response to our domain
public fun <I, E, O> Endpoint<I, E, O>.parseResponse(
  request: Request,
  response: Response
): DecodeResult<Either<E, O>> {
  val code = StatusCode(response.status.code)
  val output = if (code.isSuccess()) output else errorOutput

  val responseHeaders = response.headers
    .mapNotNull { if (it.second == null) null else it as Pair<String, String> }
    .groupBy({ it.first }) { it.second }

  val headers = response.cookies().asHeaders() + responseHeaders
  val params =
    output.getOutputParams(response, headers, code, response.status.description)

  val result = params.map { it.asAny }
    .map { p -> if (code.isSuccess()) Either.Right(p as O) else Either.Left(p as E) }

  return when (result) {
    is DecodeResult.Failure.Error ->
      DecodeResult.Failure.Error(
        result.original,
        IllegalArgumentException(
          "Cannot decode from ${result.original} of request $code - ${request.method} ${request.uri}",
          result.error
        )
      )
    else -> result
  }
}

fun List<org.http4k.core.cookie.Cookie>.asHeaders(): Map<String, List<String>> =
  mapOf("Set-Cookie" to map { c -> "${c.name}=${c.value}" })

fun EndpointOutput<*>.getOutputParams(
  response: Response,
  headers: Map<String, List<String>>,
  code: StatusCode,
  statusText: String
): DecodeResult<Params> =
  when (val output = this) {
    is EndpointOutput.Single<*> -> when (val single = (output as EndpointOutput.Single<Any?>)) {
      is EndpointIO.ByteArrayBody -> single.codec.decode(response.body.payload.array())
      is EndpointIO.ByteBufferBody -> single.codec.decode(response.body.payload)
      is EndpointIO.InputStreamBody -> single.codec.decode(response.body.stream)
      is EndpointIO.StringBody -> single.codec.decode(response.body.toString())

      is EndpointIO.Empty -> single.codec.decode(Unit)
      is EndpointOutput.FixedStatusCode -> single.codec.decode(Unit)
      is EndpointOutput.StatusCode -> single.codec.decode(code)
      is EndpointIO.Header -> single.codec.decode(headers[single.name].orEmpty())

      is EndpointIO.MappedPair<*, *, *, *> ->
        single.wrapped.getOutputParams(response, headers, code, statusText).flatMap { p ->
          (single.mapping as Mapping<Any?, DecodeResult<Any?>>).decode(p.asAny)
        }
      is EndpointOutput.MappedPair<*, *, *, *> ->
        single.output.getOutputParams(response, headers, code, statusText).flatMap { p ->
          (single.mapping as Mapping<Any?, DecodeResult<Any?>>).decode(p.asAny)
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
      "Cannot convert a void output to a value!",
      IllegalArgumentException("Cannot convert a void output to a value!")
    )
  }

private fun handleOutputPair(
  left: EndpointOutput<*>,
  right: EndpointOutput<*>,
  combine: CombineParams,
  response: Response,
  headers: Map<String, List<String>>,
  code: StatusCode,
  statusText: String
): DecodeResult<Params> {
  val l = left.getOutputParams(response, headers, code, statusText)
  val r = right.getOutputParams(response, headers, code, statusText)
  return l.flatMap { leftParams -> r.map { rightParams -> combine(leftParams, rightParams) } }
}