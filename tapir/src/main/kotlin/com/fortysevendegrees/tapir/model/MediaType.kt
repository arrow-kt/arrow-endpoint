package com.fortysevendegrees.tapir.model

import java.nio.charset.Charset

data class MediaType(val mainType: String, val subType: String, val charset: String? = null) {

  // MPP ?
  fun charset(c: Charset): MediaType = charset(c.name())

  fun charset(c: String): MediaType = copy(charset = c)

  fun noCharset(): MediaType = copy(charset = null)

  override fun toString(): String =
    "$mainType/$subType${charset?.let { c -> "; charset=$c" }}"

  // https://www.iana.org/assignments/media-types/media-types.xhtml
  companion object {
    val ApplicationGzip: MediaType = MediaType("application", "gzip")
    val ApplicationZip: MediaType = MediaType("application", "zip")
    val ApplicationJson: MediaType = MediaType("application", "json")
    val ApplicationOctetStream: MediaType = MediaType("application", "octet-stream")
    val ApplicationPdf: MediaType = MediaType("application", "pdf")
    val ApplicationRtf: MediaType = MediaType("application", "rtf")
    val ApplicationXhtml: MediaType = MediaType("application", "xhtml+xml")
    val ApplicationXml: MediaType = MediaType("application", "xml")
    val ApplicationXWwwFormUrlencoded: MediaType = MediaType("application", "x-www-form-urlencoded")

    val ImageGif: MediaType = MediaType("image", "gif")
    val ImageJpeg: MediaType = MediaType("image", "jpeg")
    val ImagePng: MediaType = MediaType("image", "png")
    val ImageTiff: MediaType = MediaType("image", "tiff")

    val MultipartFormData: MediaType = MediaType("multipart", "form-data")
    val MultipartMixed: MediaType = MediaType("multipart", "mixed")
    val MultipartAlternative: MediaType = MediaType("multipart", "alternative")

    val TextCacheManifest: MediaType = MediaType("text", "cache-manifest")
    val TextCalendar: MediaType = MediaType("text", "calendar")
    val TextCss: MediaType = MediaType("text", "css")
    val TextCsv: MediaType = MediaType("text", "csv")
    val TextEventStream: MediaType = MediaType("text", "event-stream")
    val TextJavascript: MediaType = MediaType("text", "javascript")
    val TextHtml: MediaType = MediaType("text", "html")
    val TextPlain: MediaType = MediaType("text", "plain")

    val TextPlainUtf8: MediaType = MediaType("text", "plain", "utf-8")
  }
}
