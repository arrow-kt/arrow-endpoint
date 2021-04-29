package com.fortysevendegrees.thool.ktor.server

// class KtorToResponseBody : ToResponseBody<KtorResponseBody> {
//
//  override fun fromRawValue(
//    v: Body,
//    headers: Headers,
//    format: CodecFormat
//  ): KtorResponseBody = rawValueToEntity(v, headers, format)
//
//  private fun rawValueToEntity(
//    v: Body,
//    headers: Headers,
//    format: CodecFormat
//  ): KtorResponseBody =
//    when (v) {
//      is Body.ByteArray -> ByteArrayContent(v.toByteArray(), format.toContentType(headers, null))
//      is Body.ByteBuffer -> ByteArrayContent(v.toByteArray(), format.toContentType(headers, null))
//      is Body.String -> TextContent(v.string, format.toContentType(headers, null))
//      is Body.InputStream -> OutputStreamContent(
//        {
//          v.inputStream.copyTo(this)
//        },
//        format.toContentType(headers, null)
//      )
//    }
//
//  private fun CodecFormat.toContentType(headers: Headers, charset: Charset?): ContentType =
//    headers.contentType()?.let(ContentType::parse) ?: when (this) {
//      is CodecFormat.Json -> ContentType.Application.Json
//      is CodecFormat.TextPlain ->
//        ContentType.Text.Plain
//          .withCharset(charset ?: StandardCharsets.UTF_8)
//      is CodecFormat.TextHtml ->
//        ContentType.Text.Html
//          .withCharset(charset ?: StandardCharsets.UTF_8)
//      is CodecFormat.OctetStream -> ContentType.Application.OctetStream
//      is CodecFormat.Zip -> ContentType.Application.Zip
//      is CodecFormat.XWwwFormUrlencoded -> ContentType.Application.FormUrlEncoded
//      is CodecFormat.MultipartFormData -> ContentType.MultiPart.FormData
//      CodecFormat.TextEventStream -> ContentType.Text.EventStream
//      CodecFormat.Xml -> ContentType.Application.Xml
//    }
// }
