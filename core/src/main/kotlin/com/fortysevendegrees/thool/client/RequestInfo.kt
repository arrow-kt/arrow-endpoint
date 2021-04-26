package com.fortysevendegrees.thool.client

import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.Mapping
import com.fortysevendegrees.thool.Params
import com.fortysevendegrees.thool.PlainCodec
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.Cookie
import com.fortysevendegrees.thool.model.Header
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.PathSegment
import com.fortysevendegrees.thool.model.QueryParams
import com.fortysevendegrees.thool.server.intrepreter.Body

public data class RequestInfo(
  val method: Method,
  // TODO see if we can easily use Uri here instead.
  val baseUrl: String,
  val pathSegments: List<PathSegment>,
  val queryParams: QueryParams,
  val headers: List<Header>,
  val cookies: List<Cookie>,
  val body: Body?
) {
  val baseUrlWithPath: String
    get() = baseUrl + pathSegments.joinToString(prefix = "/", separator = "/") { it.encoded() }

  val fullUrl: String
    get() = baseUrlWithPath + queryParams.ps
      .filter { it.second.isNotEmpty() }
      .joinToString(prefix = "?", separator = "&") { (name, values) ->
        values.joinToString(separator = "&") { value -> "$name=${value.replace(" ", "%20")}" }
      }
}

private fun String.trimSlash(): String =
  dropWhile { it == '/' }.dropLastWhile { it == '/' }

public fun <I> EndpointInput<I>.requestInfo(
  input: I,
  baseUrl: String // Scheme, base & port
): RequestInfo {
  val baseUrl = baseUrl.trimSlash()
  val _params = Params.ParamsAsAny(input)
  var method: Method? = null
  val pathSegments: MutableList<PathSegment> = mutableListOf()
  val queryParams: MutableList<Pair<String, List<String>>> = mutableListOf()
  val headers: MutableList<Header> = mutableListOf()
  val cookies: MutableList<Cookie> = mutableListOf()
  var body: Body? = null

  fun <I> EndpointInput<I>.buildClientInfo(params: Params): Unit {
    val value = params.asAny as I
    when (this) {
      is EndpointInput.FixedPath ->
        pathSegments.add(PathSegment(s))
      is EndpointInput.PathCapture ->
        pathSegments.add(PathSegment((codec as PlainCodec<Any?>).encode(params.asAny)))
      is EndpointInput.PathsCapture -> {
        val ps = (codec as Codec<List<String>, Any?, CodecFormat.TextPlain>).encode(params.asAny)
        pathSegments.addAll(ps.map(::PathSegment))
      }
      is EndpointIO.ByteArrayBody -> {
        body = Body.ByteArray(codec.encode(value))
      }
      is EndpointIO.ByteBufferBody -> {
        body = Body.ByteBuffer(codec.encode(value))
      }
      is EndpointIO.InputStreamBody -> {
        body = Body.InputStream(codec.encode(value))
      }
      is EndpointIO.StringBody -> {
        body = Body.String(charset, codec.encode(value))
      }

      is EndpointIO.Empty -> Unit
      is EndpointInput.FixedMethod -> {
        method = this.m
      }
      is EndpointIO.Header ->
        headers.addAll(codec.encode(value).map { v -> Header(name, v) })
      is EndpointIO.StreamBody -> TODO("Implement stream")
      is EndpointInput.Cookie -> codec.encode(value)?.let { v ->
        cookies.add(Cookie(name, v))
      }

      is EndpointInput.Query ->
        queryParams.add(Pair(name, codec.encode(value)))
      is EndpointInput.QueryParams ->
        queryParams.addAll(codec.encode(value).ps)

      // Recurse on composition of inputs.
      is EndpointInput.Pair<*, *, *> -> {
        val (leftParams, rightParams) = split(params)
        this.first.buildClientInfo(leftParams)
        this.second.buildClientInfo(rightParams)
      }
      is EndpointIO.Pair<*, *, *> -> {
        val (leftParams, rightParams) = split(params)
        this.first.buildClientInfo(leftParams)
        this.second.buildClientInfo(rightParams)
      }
      is EndpointIO.MappedPair<*, *, *, *> -> this.wrapped.buildClientInfo(
        Params.ParamsAsAny((this.mapping as Mapping<Any?, Any?>).encode(params.asAny))
      )
      is EndpointInput.MappedPair<*, *, *, *> -> this.input.buildClientInfo(
        Params.ParamsAsAny((this.mapping as Mapping<Any?, Any?>).encode(params.asAny))
      )
    }
  }

  buildClientInfo(_params)

  return RequestInfo(
    method ?: Method.GET,
    baseUrl,
    pathSegments,
    QueryParams(queryParams),
    headers,
    cookies,
    body
  )
}
