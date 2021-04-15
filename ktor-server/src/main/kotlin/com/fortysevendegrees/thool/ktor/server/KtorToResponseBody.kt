package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.RawBodyType
import kotlinx.coroutines.flow.Flow
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import com.fortysevendegrees.thool.server.intrepreter.ToResponseBody
import io.ktor.content.ByteArrayContent
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.withCharset
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.flow.collect
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class KtorToResponseBody : ToResponseBody<KtorResponseBody> {

  override fun <R> fromRawValue(
    v: R,
    headers: HasHeaders,
    format: CodecFormat,
    bodyType: RawBodyType<R>
  ): KtorResponseBody = rawValueToEntity(bodyType, headers, format, v)

  override fun fromStreamValue(
    v: Flow<Byte>,
    headers: HasHeaders,
    format: CodecFormat,
    charset: Charset?
  ): KtorResponseBody =
    ByteFlowContent(v, headers.contentLength(), format.toContentType(headers, charset))

  private fun <R> rawValueToEntity(
    bodyType: RawBodyType<R>,
    headers: HasHeaders,
    format: CodecFormat,
    r: R
  ): KtorResponseBody =
    when (bodyType) {
      is RawBodyType.StringBody -> ByteArrayContent(
        (r as String).toByteArray(bodyType.charset),
        format.toContentType(headers, bodyType.charset)
      )
      RawBodyType.ByteArrayBody ->
        ByteArrayContent(r as ByteArray, format.toContentType(headers, null))
      RawBodyType.InputStreamBody -> TODO()
      RawBodyType.ByteBufferBody -> TODO()
    }

  private fun CodecFormat.toContentType(headers: HasHeaders, charset: Charset?): ContentType =
    headers.contentType()?.let(ContentType::parse) ?: when (this) {
      is CodecFormat.Json -> ContentType.Application.Json
      is CodecFormat.TextPlain ->
        ContentType.Text.Plain
          .withCharset(charset ?: StandardCharsets.UTF_8)
      is CodecFormat.TextHtml ->
        ContentType.Text.Html
          .withCharset(charset ?: StandardCharsets.UTF_8)
      is CodecFormat.OctetStream -> ContentType.Application.OctetStream
      is CodecFormat.Zip -> ContentType.Application.Zip
      is CodecFormat.XWwwFormUrlencoded -> ContentType.Application.FormUrlEncoded
      is CodecFormat.MultipartFormData -> ContentType.MultiPart.FormData
      CodecFormat.TextEventStream -> ContentType.Text.EventStream
      CodecFormat.Xml -> ContentType.Application.Xml
    }
}

class ByteFlowContent(
  val flow: Flow<Byte>,
  override val contentLength: Long?,
  override val contentType: ContentType
) : OutgoingContent.WriteChannelContent() {

  override suspend fun writeTo(channel: ByteWriteChannel): Unit {
    flow.collect(channel::writeByte)
  }
}
