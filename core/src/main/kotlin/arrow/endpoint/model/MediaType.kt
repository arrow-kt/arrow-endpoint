package arrow.endpoint.model

import java.nio.charset.Charset

public data class MediaType(
  val mainType: String,
  val subType: String,
  val charset: String? = null
) {

  // TODO kotlinx-io-core offers a MPP Charset implementation.
  // Only offers UTF_8 & ISO_8859_1
  public fun charset(c: Charset): MediaType = charset(c.name())

  public fun charset(c: String): MediaType = copy(charset = c)

  public fun noCharset(): MediaType = copy(charset = null)

  override fun toString(): String =
    "$mainType/$subType${charset?.let { c -> "; charset=$c" } ?: ""}"

  // https://www.iana.org/assignments/media-types/media-types.xhtml
  public companion object {
    public val ApplicationGzip: MediaType = MediaType("application", "gzip")
    public val ApplicationZip: MediaType = MediaType("application", "zip")
    public val ApplicationJson: MediaType = MediaType("application", "json")
    public val ApplicationOctetStream: MediaType = MediaType("application", "octet-stream")
    public val ApplicationPdf: MediaType = MediaType("application", "pdf")
    public val ApplicationRtf: MediaType = MediaType("application", "rtf")
    public val ApplicationXhtml: MediaType = MediaType("application", "xhtml+xml")
    public val ApplicationXml: MediaType = MediaType("application", "xml")
    public val ApplicationXWwwFormUrlencoded: MediaType =
      MediaType("application", "x-www-form-urlencoded")

    public val ImageGif: MediaType = MediaType("image", "gif")
    public val ImageJpeg: MediaType = MediaType("image", "jpeg")
    public val ImagePng: MediaType = MediaType("image", "png")
    public val ImageTiff: MediaType = MediaType("image", "tiff")

    public val MultipartFormData: MediaType = MediaType("multipart", "form-data")
    public val MultipartMixed: MediaType = MediaType("multipart", "mixed")
    public val MultipartAlternative: MediaType = MediaType("multipart", "alternative")

    public val TextCacheManifest: MediaType = MediaType("text", "cache-manifest")
    public val TextCalendar: MediaType = MediaType("text", "calendar")
    public val TextCss: MediaType = MediaType("text", "css")
    public val TextCsv: MediaType = MediaType("text", "csv")
    public val TextEventStream: MediaType = MediaType("text", "event-stream")
    public val TextJavascript: MediaType = MediaType("text", "javascript")
    public val TextHtml: MediaType = MediaType("text", "html")
    public val TextPlain: MediaType = MediaType("text", "plain")

    public val TextPlainUtf8: MediaType = MediaType("text", "plain", "utf-8")
  }
}
