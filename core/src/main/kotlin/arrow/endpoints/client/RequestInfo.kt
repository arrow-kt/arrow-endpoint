
package arrow.endpoint.client

import arrow.endpoint.Codec
import arrow.endpoint.EndpointIO
import arrow.endpoint.EndpointInput
import arrow.endpoint.Mapping
import arrow.endpoint.Params
import arrow.endpoint.PlainCodec
import arrow.endpoint.model.Body
import arrow.endpoint.model.CodecFormat
import arrow.endpoint.model.Cookie
import arrow.endpoint.model.Header
import arrow.endpoint.model.Method
import arrow.endpoint.model.PathSegment
import arrow.endpoint.model.QueryParams

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
  @Suppress("NAME_SHADOWING")
  val baseUrl = baseUrl.trimSlash()
  var method: Method? = null
  val pathSegments: MutableList<PathSegment> = mutableListOf()
  val queryParams: MutableList<Pair<String, List<String>>> = mutableListOf()
  val headers: MutableList<Header> = mutableListOf()
  val cookies: MutableList<Cookie> = mutableListOf()
  var body: Body? = null

  @Suppress("UNCHECKED_CAST")
  fun <I> EndpointInput<I>.buildClientInfo(params: Params) {
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
        body = Body.ByteArray(codec.encode(value), codec.format)
      }
      is EndpointIO.ByteBufferBody -> {
        body = Body.ByteBuffer(codec.encode(value), codec.format)
      }
      is EndpointIO.InputStreamBody -> {
        body = Body.InputStream(codec.encode(value), codec.format)
      }
      is EndpointIO.StringBody -> {
        body = Body.String(charset, codec.encode(value), codec.format)
      }

      is EndpointIO.Empty -> Unit
      is EndpointInput.FixedMethod -> {
        method = this.m
      }
      is EndpointIO.Header ->
        headers.addAll(codec.encode(value).map { v -> Header(name, v) })
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

  buildClientInfo(Params.ParamsAsAny(input))

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
