package com.fortysevendegrees.thool.ktor.client

import arrow.core.Either
import com.fortysevendegrees.thool.CombineParams
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointOutput
import com.fortysevendegrees.thool.Mapping
import com.fortysevendegrees.thool.Params
import com.fortysevendegrees.thool.client.requestInfo
import com.fortysevendegrees.thool.model.StatusCode
import com.fortysevendegrees.thool.server.interpreter.Body
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.cookie
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.client.utils.EmptyContent
import io.ktor.content.ByteArrayContent
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.takeFrom
import java.nio.ByteBuffer

fun <I, E, O> Endpoint<I, E, O>.requestAndParse(
  baseUrl: String
): suspend HttpClient.(I) -> DecodeResult<Either<E, O>> =
  { value: I ->
    val response = invoke(this@requestAndParse, baseUrl)(value)
    this@requestAndParse.responseToDomain(response)
  }

operator fun <I, E, O> HttpClient.invoke(
  endpoint: Endpoint<I, E, O>,
  baseUrl: String
): suspend (I) -> HttpResponse = { value ->
  val info = endpoint.input.requestInfo(value, baseUrl)
  request {
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
}

@Suppress("UNCHECKED_CAST")
suspend fun <I, E, O> Endpoint<I, E, O>.responseToDomain(
  response: HttpResponse
): DecodeResult<Either<E, O>> {
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

@Suppress("UNCHECKED_CAST")
suspend fun EndpointOutput<*>.outputParams(
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
        is EndpointIO.StreamBody -> TODO() // (output.codec::decode as (Any?) -> DecodeResult<Params>).invoke(body())
        is EndpointOutput.FixedStatusCode -> codec.decode(Unit)
        is EndpointOutput.StatusCode -> codec.decode(code)
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
