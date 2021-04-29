package com.fortysevendegrees.thool.model

import arrow.core.nonFatalOrThrow

public fun List<Header>.toStringSafe(sensitiveHeaders: Set<String> = Headers.SensitiveHeaders): List<String> =
  map { it.toStringSafe(sensitiveHeaders) }

public interface Headers {
  public val headers: List<Header>

  public fun header(h: String): String? =
    headers.firstOrNull { it.hasName(h) }?.value

  public fun headers(h: String): List<String> =
    headers.filter { it.hasName(h) }.map { it.value }

  public fun contentType(): String? =
    header(ContentType)

  public fun contentLength(): Long? =
    header(ContentLength)?.let { cl ->
      try {
        cl.toLong()
      } catch (e: Throwable) {
        e.nonFatalOrThrow(); null
      }
    }

//  fun cookies(): List<Either<String, CookieWithMeta>> = headers(HeaderNames.SetCookie).com.fortysevendegrees.tapir.map(h => CookieWithMeta.parse(h))
//  def unsafeCookies: Seq[CookieWithMeta] = cookies.com.fortysevendegrees.tapir.map(_.fold(e => throw new RuntimeException(e), identity[CookieWithMeta]))

  public companion object {
    public operator fun invoke(headers: List<Header>): Headers = HeadersImpl(headers)

    // https://www.iana.org/assignments/message-headers/message-headers.xml#perm-headers
    public const val Accept: String = "Accept"
    public const val AcceptCharset: String = "Accept-Charset"
    public const val AcceptEncoding: String = "Accept-Encoding"
    public const val AcceptLanguage: String = "Accept-Language"
    public const val AcceptRanges: String = "Accept-Ranges"
    public const val AccessControlAllowCredentials: String = "Access-Control-Allow-Credentials"
    public const val AccessControlAllowHeaders: String = "Access-Control-Allow-Headers"
    public const val AccessControlAllowMethods: String = "Access-Control-Allow-Methods"
    public const val AccessControlAllowOrigin: String = "Access-Control-Allow-Origin"
    public const val AccessControlExposeHeaders: String = "Access-Control-Expose-Headers"
    public const val AccessControlMaxAge: String = "Access-Control-Max-Age"
    public const val AccessControlRequestHeaders: String = "Access-Control-Request-Headers"
    public const val AccessControlRequestMethod: String = "Access-Control-Request-Method"
    public const val Age: String = "Age"
    public const val Allow: String = "Allow"
    public const val Authorization: String = "Authorization"
    public const val CacheControl: String = "Cache-Control"
    public const val Connection: String = "Connection"
    public const val ContentDisposition: String = "Content-Disposition"
    public const val ContentEncoding: String = "Content-Encoding"
    public const val ContentLanguage: String = "Content-Language"
    public const val ContentLength: String = "Content-Length"
    public const val ContentLocation: String = "Content-Location"
    public const val ContentMd5: String = "Content-MD5"
    public const val ContentRange: String = "Content-Range"
    public const val ContentTransferEncoding: String = "Content-Transfer-Encoding"
    public const val ContentType: String = "Content-Type"
    public const val Cookie: String = "Cookie"
    public const val Date: String = "Date"
    public const val Etag: String = "ETag"
    public const val Expect: String = "Expect"
    public const val Expires: String = "Expires"
    public const val Forwarded: String = "Forwarded"
    public const val From: String = "From"
    public const val Host: String = "Host"
    public const val IfMatch: String = "If-Match"
    public const val IfModifiedSince: String = "If-Modified-Since"
    public const val IfNoneMatch: String = "If-None-Match"
    public const val IfRange: String = "If-Range"
    public const val IfUnmodifiedSince: String = "If-Unmodified-Since"
    public const val LastModified: String = "Last-Modified"
    public const val Link: String = "Link"
    public const val Location: String = "Location"
    public const val MaxForwards: String = "Max-Forwards"
    public const val Origin: String = "Origin"
    public const val Pragma: String = "Pragma"
    public const val ProxyAuthenticate: String = "Proxy-Authenticate"
    public const val ProxyAuthorization: String = "Proxy-Authorization"
    public const val Range: String = "Range"
    public const val Referer: String = "Referer"
    public const val RemoteAddress: String = "Remote-Address"
    public const val RetryAfter: String = "Retry-After"
    public const val Server: String = "Server"
    public const val SetCookie: String = "Set-Cookie"
    public const val StrictTransportSecurity: String = "Strict-Transport-Security"
    public const val Te: String = "Te"
    public const val Trailer: String = "Trailer"
    public const val TransferEncoding: String = "Transfer-Encoding"
    public const val Upgrade: String = "Upgrade"
    public const val UserAgent: String = "User-Agent"
    public const val Vary: String = "Vary"
    public const val Via: String = "Via"
    public const val Warning: String = "Warning"
    public const val WwwAuthenticate: String = "WWW-Authenticate"
    public const val XFrameOptions: String = "X-Frame-Options"
    public const val XForwardedFor: String = "X-Forwarded-For"
    public const val XForwardedHost: String = "X-Forwarded-Host"
    public const val XForwardedPort: String = "X-Forwarded-Port"
    public const val XForwardedProto: String = "X-Forwarded-Proto"
    public const val XRealIp: String = "X-Real-Ip"
    public const val XRequestedWith: String = "X-Requested-With"
    public const val XXSSProtection: String = "X-XSS-Protection"

    public val ContentHeaders: Set<String> =
      setOf(ContentLength, ContentType, ContentMd5).map(String::toLowerCase).toSet()

    public val SensitiveHeaders: Set<String> =
      setOf(Authorization, Cookie, SetCookie).map(String::toLowerCase).toSet()

    /** Performs a case-insensitive check, whether this header name is content-related.
     */
    public fun isContent(headerName: String): Boolean =
      ContentHeaders.contains(headerName.toLowerCase().trim())

    /** Performs a case-insensitive check, whether this header is content-related.
     */
    public fun isContent(header: Header): Boolean =
      isContent(header.name)

    /** Performs a case-insensitive check, whether this header name is sensitive.
     */
    public fun isSensitive(headerName: String): Boolean =
      isSensitive(headerName, SensitiveHeaders)

    /** Performs a case-insensitive check, whether this header name is sensitive.
     */
    public fun isSensitive(headerName: String, sensitiveHeaders: Set<String>): Boolean =
      sensitiveHeaders.map(String::toLowerCase).contains(headerName.toLowerCase().trim())

    /** Performs a case-insensitive check, whether this header is sensitive.
     */
    public fun isSensitive(header: Header): Boolean =
      isSensitive(header.name, SensitiveHeaders)

    /** Performs a case-insensitive check, whether this header is sensitive.
     */
    public fun isSensitive(header: Header, sensitiveHeaders: Set<String>): Boolean =
      isSensitive(header.name, sensitiveHeaders)
  }
}

private data class HeadersImpl(override val headers: List<Header>) :
  Headers {
  override fun toString(): String = "Headers(${headers.toStringSafe()}})"
}
