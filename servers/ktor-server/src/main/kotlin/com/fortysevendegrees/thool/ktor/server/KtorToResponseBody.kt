package com.fortysevendegrees.thool.ktor.server

import kotlinx.coroutines.flow.Flow
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import com.fortysevendegrees.thool.server.interpreter.Body
import com.fortysevendegrees.thool.server.interpreter.ByteArrayBody
import com.fortysevendegrees.thool.server.interpreter.ByteBufferBody
import com.fortysevendegrees.thool.server.interpreter.InputStreamBody
import com.fortysevendegrees.thool.server.interpreter.StringBody
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
    v: Body,
    headers: HasHeaders,
    format: CodecFormat
  ): KtorResponseBody = rawValueToEntity(v, headers, format)

  override fun fromStreamValue(
    v: Flow<Byte>,
    headers: HasHeaders,
    format: CodecFormat,
    charset: Charset?
  ): KtorResponseBody =
    ByteFlowContent(v, headers.contentLength(), format.toContentType(headers, charset))

  private fun ByteBuffer.moveToByteArray(): ByteArray {
    val array = ByteArray(remaining())
    get(array)
    return array
  }

  private fun rawValueToEntity(
    v: Body,
    headers: HasHeaders,
    format: CodecFormat
  ): KtorResponseBody =
    when (v) {
      is ByteArrayBody -> ByteArrayContent(v.byteArray, format.toContentType(headers, null))
      is ByteBufferBody -> ByteArrayContent(v.byteBuffer.moveToByteArray(), format.toContentType(headers, null))
      is StringBody -> ByteArrayContent(v.string.toByteArray(v.charset))
      is InputStreamBody -> OutputStreamContent(
        {
          v.inputStream.copyTo(this)
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
