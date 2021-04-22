package com.fortysevendegrees.thool.ktor.client

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
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.StatusCode
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.cookie
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.content.ByteArrayContent
import io.ktor.http.Headers
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.parametersOf
import io.ktor.http.plus
import io.ktor.http.takeFrom
import java.io.InputStream
import java.nio.ByteBuffer

fun <I, E, O> Endpoint<I, E, O>.requestAndParse(
  baseUrl: String
): suspend HttpClient.(I) -> DecodeResult<Either<E, O>> =
  { value: I ->
    val response = invoke(this@requestAndParse, baseUrl)(value)
    println("######################### reponse = $response")
    this@requestAndParse.responseToDomain(response)
  }

operator fun <I, E, O> HttpClient.invoke(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String
): suspend (I) -> HttpResponse = { value ->
  val p = Params.ParamsAsAny(value)
  request {
    url.takeFrom(endpoint.input.buildUrl(baseUrl, p))
    setInputParams(endpoint.input, p)
    method = (endpoint.input.method() ?: Method.GET).toMethod()
  }
}

@Suppress("UNCHECKED_CAST")
suspend fun <I, E, O> Endpoint<I, E, O>.responseToDomain(
  response: HttpResponse
): DecodeResult<Either<E, O>> {
  fun EndpointOutput<*>.responseFromOutput(
    response: HttpResponse
  ): suspend () -> Any = {
    // TODO: StreamBody is missing maybe take advantage over type conversion in `receive`
    when (bodyType()) {
      RawBodyType.ByteArrayBody -> response.receive<ByteArray>()
      RawBodyType.ByteBufferBody -> response.receive<ByteBuffer>()
      RawBodyType.InputStreamBody -> response.receive<InputStream>()
      is RawBodyType.StringBody -> response.receive<String>()
      null -> Unit
    }
  }

  val code = StatusCode(response.status.value)
  val text = response.status.description
  val output = if (code.isSuccess()) output else errorOutput
  val body = output.responseFromOutput(response)
  val headers = response.headers
  val params = output.outputParams(body, headers, code, text)
  val result = params.map {
    val v = it.asAny
    if (code.isSuccess()) Either.Right(v as O) else Either.Left(v as E)
  }
  return when (result) {
    is DecodeResult.Failure.Error -> {
      DecodeResult.Failure.Error(
        result.original,
        IllegalArgumentException(
          "Cannot decode from ${result.original} of request ${response.request.method} ${response.request.url}",
          result.error
        )
      )
    }
    else -> result
  }
}

@Suppress("UNCHECKED_CAST")
suspend fun EndpointOutput<*>.outputParams(
  body: suspend () -> Any?,
  headers: Headers,
  code: StatusCode,
  codeDescription: String
): DecodeResult<Params> {
  suspend fun handleOutputPair(
    left: EndpointOutput<*>,
    right: EndpointOutput<*>,
    combine: CombineParams,
    body: suspend () -> Any?,
    headers: Headers,
    code: StatusCode,
    codeDescription: String
  ): DecodeResult<Params> {
    val l = left.outputParams(body, headers, code, codeDescription)
    val r = right.outputParams(body, headers, code, codeDescription)
    return l.flatMap { ll -> r.map { rr -> combine(ll, rr) } }
  }
  return when (this) {
    is EndpointOutput.Single<*> ->
      when (this) {
        is EndpointIO.Body<*, *> -> {
          this as EndpointIO.Body<Any?, Any?>
          val b = body()
          val f = codec::decode
          f(b)
        }
        is EndpointIO.Empty -> codec.decode(Unit)
        is EndpointIO.Header -> codec.decode(headers.getAll(name).orEmpty())
        is EndpointIO.StreamBody -> TODO() // (output.codec::decode as (Any?) -> DecodeResult<Params>).invoke(body())
        is EndpointOutput.FixedStatusCode -> codec.decode(Unit)
        is EndpointOutput.StatusCode -> codec.decode(code)
        is EndpointOutput.MappedPair<*, *, *, *> ->
          output.outputParams(body, headers, code, codeDescription).flatMap {
            (mapping::decode as (Any?) -> DecodeResult<Any?>)(it.asAny)
          }
        is EndpointIO.MappedPair<*, *, *, *> ->
          wrapped.outputParams(body, headers, code, codeDescription).flatMap { p ->
            (mapping::decode as (Any?) -> DecodeResult<Any?>)(p.asAny)
          }
      }.map(Params::ParamsAsAny)
    is EndpointIO.Pair<*, *, *> -> handleOutputPair(first, second, combine, body, headers, code, codeDescription)
    is EndpointOutput.Pair<*, *, *> -> handleOutputPair(first, second, combine, body, headers, code, codeDescription)
    is EndpointOutput.Void -> DecodeResult.Failure.Error(
      "",
      IllegalArgumentException("Cannot convert a void output to a value!")
    )
  }
}

@Suppress("UNCHECKED_CAST")
fun <I> HttpRequestBuilder.setInputParams(input: EndpointInput<I>, params: Params): Unit =
  (params.asAny as I).let { value ->
    fun <I> setBody(value: I, codec: Codec<*, *, CodecFormat>, rawBodyType: RawBodyType<*>): Unit =
      when (rawBodyType) {
        RawBodyType.ByteArrayBody -> {
          body = ByteArrayContent((codec::encode as (I) -> ByteArray)(value))
        }
        RawBodyType.ByteBufferBody -> {
          body = (codec::encode as (I) -> ByteBuffer)(value)
        }
        RawBodyType.InputStreamBody -> {
          body = (codec::encode as (I) -> InputStream)(value)
        }
        is RawBodyType.StringBody -> {
          body = (codec::encode as (I) -> String)(value)
        }
      }

    fun handleInputPair(left: EndpointInput<*>, right: EndpointInput<*>, params: Params, split: SplitParams): Unit =
      split(params).let { (leftParams, rightParams) ->
        setInputParams(left, leftParams)
        setInputParams(right, rightParams)
      }

    fun handleMapped(input: EndpointInput<*>, mapped: Mapping<*, *>, params: Params): Unit =
      setInputParams(input, Params.ParamsAsAny((mapped::encode as (Any?) -> Any)(params.asAny)))

    when (input) {
      is EndpointIO.Body<*, *> -> setBody(value, input.codec, input.bodyType)
      is EndpointIO.Empty -> Unit
      is EndpointIO.Header ->
        headers.appendAll(input.name, input.codec.encode(value))
      is EndpointIO.StreamBody -> TODO("implement stream")
      is EndpointInput.Cookie -> input.codec.encode(value)?.let { cookie(input.name, it) }
      is EndpointInput.Query ->
        // appendMissing bc `url.takeFrom(String)` can already parse query segments
        url.parameters.appendMissing(input.name, input.codec.encode(value))
      is EndpointInput.QueryParams ->
        // appendMissing bc `url.takeFrom(String)` can already parse query segments
        url.parameters.appendMissing(
          input.codec.encode(value).ps.fold(Parameters.Empty) { p, (name, v) ->
            p + parametersOf(name, v)
          }
        )
      // recursive
      is EndpointIO.MappedPair<*, *, *, *> -> handleMapped(input, input.mapping, params)
      is EndpointInput.MappedPair<*, *, *, *> -> handleMapped(input, input.mapping, params)
      is EndpointInput.Pair<*, *, *> -> handleInputPair(input.first, input.second, params, input.split)
      is EndpointIO.Pair<*, *, *> -> handleInputPair(input.first, input.second, params, input.split)
      // already inserted
      is EndpointInput.FixedMethod,
      is EndpointInput.FixedPath,
      is EndpointInput.PathCapture,
      is EndpointInput.PathsCapture -> Unit
    }
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
