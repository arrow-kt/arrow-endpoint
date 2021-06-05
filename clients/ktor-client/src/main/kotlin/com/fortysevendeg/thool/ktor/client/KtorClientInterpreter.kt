package com.fortysevendeg.thool.ktor.client

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
import com.fortysevendeg.thool.model.Method
import com.fortysevendeg.thool.model.StatusCode
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.cookie
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.request
import io.ktor.client.utils.EmptyContent
import io.ktor.content.ByteArrayContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.takeFrom
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

public suspend operator fun <I, E, O> HttpClient.invoke(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): DecodeResult<Either<E, O>> {
  val request = endpoint.toRequestBuilder(baseUrl, input)
  val response = HttpStatement(request, this).execute()
  return endpoint.parseResponse(response)
}

public suspend fun <I, E, O> HttpClient.execute(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String,
  input: I
): Triple<HttpRequestData, HttpResponse, DecodeResult<Either<E, O>>> {
  val request = endpoint.toRequestBuilder(baseUrl, input)
  val response = HttpStatement(request, this).execute()
  val result = endpoint.parseResponse(response)
  return Triple(request.build(), response, result)
}

public fun <I, E, O> Endpoint<I, E, O>.toRequestBuilder(baseUrl: String, input: I): HttpRequestBuilder =
  HttpRequestBuilder().apply {
    val info = this@toRequestBuilder.input.requestInfo(input, baseUrl)
    method = info.method.toMethod()
    url.takeFrom(info.baseUrlWithPath)
    info.cookies.forEach { (name, value) ->
      cookie(name, value)
    }
    info.headers.forEach { (name, value) ->
      headers.append(name, value)
    }
    info.queryParams.ps.forEach { (name, params) ->
      url.parameters.appendAll(name, params)
    }
    body = when (val body = info.body) {
      is Body.ByteArray -> ByteArrayContent(body.byteArray/* contentType,  statusCode*/)
      is Body.ByteBuffer -> ByteArrayContent(body.byteBuffer.array())
      is Body.InputStream -> ByteArrayContent(body.inputStream.readBytes())

      // TODO fix ContentType
      is Body.String -> TextContent(body.string, ContentType.Text.Plain)
      null -> EmptyContent
    }
  }

@Suppress("UNCHECKED_CAST")
public suspend fun <I, E, O> Endpoint<I, E, O>.parseResponse(response: HttpResponse): DecodeResult<Either<E, O>> {
  val code = StatusCode(response.status.value)
  val output = if (code.isSuccess()) output else errorOutput
  val headers = response.headers
  val params = output.outputParams(response, headers, code)
  val result = params.map {
    val v = it.asAny
    if (code.isSuccess()) Either.Right(v as O) else Either.Left(v as E)
  }
  return when (result) {
    is DecodeResult.Failure.Error -> {
      DecodeResult.Failure.Error(
        result.original,
        IllegalArgumentException(
          "Cannot decode from ${result.original} of request $code - ${response.request.method} ${response.request.url}",
          result.error
        )
      )
    }
    else -> result
  }
}

public fun Method.toMethod(): HttpMethod =
  when (this) {
    Method.GET -> HttpMethod.Get
    Method.HEAD -> HttpMethod.Head
    Method.POST -> HttpMethod.Post
    Method.PUT -> HttpMethod.Put
    Method.DELETE -> HttpMethod.Delete
    Method.OPTIONS -> HttpMethod.Options
    Method.PATCH -> HttpMethod.Patch
    Method.CONNECT -> HttpMethod("CONNECT")
    Method.TRACE -> HttpMethod("TRACE")
    else -> HttpMethod(value)
  }

@Suppress("UNCHECKED_CAST")
private suspend fun EndpointOutput<*>.outputParams(
  response: HttpResponse,
  headers: Headers,
  code: StatusCode
): DecodeResult<Params> {
  suspend fun handleOutputPair(
    left: EndpointOutput<*>,
    right: EndpointOutput<*>,
    combine: CombineParams,
    response: HttpResponse,
    headers: Headers,
    code: StatusCode
  ): DecodeResult<Params> {
    val l = left.outputParams(response, headers, code)
    val r = right.outputParams(response, headers, code)
    return l.flatMap { ll -> r.map { rr -> combine(ll, rr) } }
  }
  return when (this) {
    is EndpointOutput.Single<*> ->
      when (this) {
        is EndpointIO.ByteArrayBody -> codec.decode(response.receive())
        is EndpointIO.ByteBufferBody -> codec.decode(ByteBuffer.wrap(response.receive<ByteArray>()))
        is EndpointIO.InputStreamBody -> codec.decode(response.receive())
        is EndpointIO.StringBody -> codec.decode(response.receive())
        is EndpointIO.Empty -> codec.decode(Unit)
        is EndpointIO.Header -> codec.decode(headers.getAll(name).orEmpty())
        is EndpointOutput.FixedStatusCode -> codec.decode(Unit)
        is EndpointOutput.StatusCode -> codec.decode(code)
        is EndpointOutput.OneOf<*, *> -> mappings.firstOrNull { it.statusCode == null || it.statusCode == code }
          ?.let { mapping -> mapping.output.outputParams(response, headers, code).flatMap { p -> (codec as Mapping<Any?, Any?>).decode(p.asAny) } }
          ?: DecodeResult.Failure.Error(response.status.description, IllegalArgumentException("Cannot find mapping for status code $code in outputs $this"))

        is EndpointOutput.MappedPair<*, *, *, *> ->
          output.outputParams(response, headers, code).flatMap { p ->
            (mapping as Mapping<Any?, Any?>).decode(p.asAny)
          }
        is EndpointIO.MappedPair<*, *, *, *> ->
          wrapped.outputParams(response, headers, code).flatMap { p ->
            (mapping as Mapping<Any?, Any?>).decode(p.asAny)
          }
      }.map(Params::ParamsAsAny)
    is EndpointIO.Pair<*, *, *> -> handleOutputPair(first, second, combine, response, headers, code)
    is EndpointOutput.Pair<*, *, *> -> handleOutputPair(
      first,
      second,
      combine,
      response,
      headers,
      code
    )
    is EndpointOutput.Void -> DecodeResult.Failure.Error(
      "",
      IllegalArgumentException("Cannot convert a void output to a value!")
    )
  }
}
