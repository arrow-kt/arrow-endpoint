package arrow.endpoint.ktor.server

import arrow.endpoint.EndpointIO
import arrow.endpoint.model.Address
import arrow.endpoint.model.Authority
import arrow.endpoint.model.Body
import arrow.endpoint.model.CodecFormat
import arrow.endpoint.model.ConnectionInfo
import arrow.endpoint.model.Header
import arrow.endpoint.model.HostSegment
import arrow.endpoint.model.Method
import arrow.endpoint.model.PathSegments
import arrow.endpoint.model.QuerySegment
import arrow.endpoint.model.ServerRequest
import arrow.endpoint.model.ServerResponse
import arrow.endpoint.model.Uri
import arrow.endpoint.server.ServerEndpoint
import arrow.endpoint.server.interpreter.RequestBody
import arrow.endpoint.server.interpreter.ServerInterpreter
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.RequestConnectionPoint
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.OutputStreamContent
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.request.host
import io.ktor.request.httpMethod
import io.ktor.request.httpVersion
import io.ktor.request.path
import io.ktor.request.port
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.util.flattenEntries
import io.ktor.util.toByteArray
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

public fun <I, E, O> Application.install(ses: ServerEndpoint<I, E, O>): Unit =
  install(listOf(ses))

public fun Application.install(ses: List<ServerEndpoint<*, *, *>>): Unit =
  intercept(ApplicationCallPipeline.ApplicationPhase.Call) {
    val interpreter = ServerInterpreter(
      call.toServerRequest(),
      KtorRequestBody(call),
      emptyList()
    )

    interpreter.invoke(ses)?.let {
      it.headers.forEach { (name, value) ->
        // Header(s) Content-Type are controlled by the engine and cannot be set explicitly
        if (name != Header.ContentType) call.response.header(name, value)
      }

      when (val body = it.outgoingContent()) {
        null -> call.respond(HttpStatusCode.fromValue(it.code.code))
        else -> call.respond(HttpStatusCode.fromValue(it.code.code), body)
      }
    }
  }

public fun ServerResponse.outgoingContent(): OutgoingContent? =
  when (val body = body) {
    is Body.ByteArray -> ByteArrayContent(
      body.toByteArray(),
      body.contentType(),
      HttpStatusCode.fromValue(code.code)
    )
    is Body.ByteBuffer -> ByteArrayContent(
      body.toByteArray(),
      body.contentType(),
      HttpStatusCode.fromValue(code.code)
    )
    is Body.String -> TextContent(
      body.string,
      body.contentType(),
      HttpStatusCode.fromValue(code.code)
    )
    is Body.InputStream -> OutputStreamContent(
      {
        body.inputStream.copyTo(this)
      },
      body.contentType(),
      HttpStatusCode.fromValue(code.code)
    )
    else -> null
  }

private fun Body.contentType(): ContentType =
  when (format) {
    is CodecFormat.Json -> ContentType.Application.Json
    is CodecFormat.TextPlain -> ContentType.Text.Plain.withCharset(charsetOrNull() ?: StandardCharsets.UTF_8)
    is CodecFormat.TextHtml -> ContentType.Text.Html.withCharset(charsetOrNull() ?: StandardCharsets.UTF_8)
    is CodecFormat.OctetStream -> ContentType.Application.OctetStream
    is CodecFormat.Zip -> ContentType.Application.Zip
    is CodecFormat.XWwwFormUrlencoded -> ContentType.Application.FormUrlEncoded
    is CodecFormat.MultipartFormData -> ContentType.MultiPart.FormData
    CodecFormat.TextEventStream -> ContentType.Text.EventStream
    CodecFormat.Xml -> ContentType.Application.Xml
  }

public fun ApplicationCall.toServerRequest(): ServerRequest {
  val uri = Uri(
    request.origin.scheme,
    Authority(null, HostSegment(request.host()), request.port()),
    PathSegments.absoluteOrEmptyS(request.path().removePrefix("/").split("/")),
    request.queryParameters.entries().flatMap { (name, values) ->
      values.map { QuerySegment.KeyValue(name, it) }
    },
    null
  )
  return ServerRequest(
    protocol = request.httpVersion,
    connectionInfo = ConnectionInfo(request.origin.toAddress(), null, null),
    method = Method(request.httpMethod.value),
    uri = uri,
    headers = request.headers.flattenEntries().map { (name, value) -> Header(name, value) },
    pathSegments = uri.path(),
    queryParameters = uri.params()
  )
}

private fun RequestConnectionPoint.toAddress(): Address = Address(host, port)

internal class KtorRequestBody(private val ctx: ApplicationCall) : RequestBody {
  @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
  override suspend fun <R> toRaw(bodyType: EndpointIO.Body<R, *>): R {
    val body = ctx.request.receiveChannel()
    return when (bodyType) {
      is EndpointIO.ByteArrayBody -> body.toByteArray()
      is EndpointIO.ByteBufferBody -> ByteBuffer.wrap(body.toByteArray())
      is EndpointIO.InputStreamBody -> body.toInputStream()
      is EndpointIO.StringBody -> body.toByteArray().toString(bodyType.charset)
    } as R
  }
}
