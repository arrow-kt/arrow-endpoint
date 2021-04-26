package com.fortysevendegrees.thool.ktor.server

import kotlinx.coroutines.flow.Flow
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import com.fortysevendegrees.thool.server.intrepreter.Body
import com.fortysevendegrees.thool.server.interpreter.ToResponseBody
import io.ktor.content.ByteArrayContent
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.OutputStreamContent
import io.ktor.http.withCharset
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.flow.collect
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class KtorToResponseBody : ToResponseBody<KtorResponseBody> {

  override fun fromRawValue(
    raw: Body,
    headers: HasHeaders,
    format: CodecFormat
  ): KtorResponseBody = rawValueToEntity(raw, headers, format)

  override fun fromStreamValue(
    raw: Flow<Byte>,
    headers: HasHeaders,
    format: CodecFormat,
    charset: Charset?
  ): KtorResponseBody =
    ByteFlowContent(raw, headers.contentLength(), format.toContentType(headers, charset))

  private fun ByteBuffer.moveToByteArray(): ByteArray {
    val array = ByteArray(remaining())
    get(array)
    return array
  }

  private fun rawValueToEntity(
    body: Body,
    headers: HasHeaders,
    format: CodecFormat
  ): KtorResponseBody =
    when (body) {
      is Body.ByteArray -> ByteArrayContent(body.byteArray, format.toContentType(headers, null))
      is Body.ByteBuffer -> ByteArrayContent(body.byteBuffer.moveToByteArray(), format.toContentType(headers, null))
      is Body.String -> ByteArrayContent(body.string.toByteArray(body.charset))
      is Body.InputStream -> OutputStreamContent(
        {
          body.inputStream.copyTo(this)
        },
        format.toContentType(headers, null)
      )
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
