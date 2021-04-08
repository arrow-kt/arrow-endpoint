package com.fortysevendegrees.thool.model

// https://www.iana.org/assignments/message-headers/message-headers.xml#perm-headers
object HeaderNames {
  const val Accept: String = "Accept"
  const val AcceptCharset: String = "Accept-Charset"
  const val AcceptEncoding: String = "Accept-Encoding"
  const val AcceptLanguage: String = "Accept-Language"
  const val AcceptRanges: String = "Accept-Ranges"
  const val AccessControlAllowCredentials: String = "Access-Control-Allow-Credentials"
  const val AccessControlAllowHeaders: String = "Access-Control-Allow-Headers"
  const val AccessControlAllowMethods: String = "Access-Control-Allow-Methods"
  const val AccessControlAllowOrigin: String = "Access-Control-Allow-Origin"
  const val AccessControlExposeHeaders: String = "Access-Control-Expose-Headers"
  const val AccessControlMaxAge: String = "Access-Control-Max-Age"
  const val AccessControlRequestHeaders: String = "Access-Control-Request-Headers"
  const val AccessControlRequestMethod: String = "Access-Control-Request-Method"
  const val Age: String = "Age"
  const val Allow: String = "Allow"
  const val Authorization: String = "Authorization"
  const val CacheControl: String = "Cache-Control"
  const val Connection: String = "Connection"
  const val ContentDisposition: String = "Content-Disposition"
  const val ContentEncoding: String = "Content-Encoding"
  const val ContentLanguage: String = "Content-Language"
  const val ContentLength: String = "Content-Length"
  const val ContentLocation: String = "Content-Location"
  const val ContentMd5: String = "Content-MD5"
  const val ContentRange: String = "Content-Range"
  const val ContentTransferEncoding: String = "Content-Transfer-Encoding"
  const val ContentType: String = "Content-Type"
  const val Cookie: String = "Cookie"
  const val Date: String = "Date"
  const val Etag: String = "ETag"
  const val Expect: String = "Expect"
  const val Expires: String = "Expires"
  const val Forwarded: String = "Forwarded"
  const val From: String = "From"
  const val Host: String = "Host"
  const val IfMatch: String = "If-Match"
  const val IfModifiedSince: String = "If-Modified-Since"
  const val IfNoneMatch: String = "If-None-Match"
  const val IfRange: String = "If-Range"
  const val IfUnmodifiedSince: String = "If-Unmodified-Since"
  const val LastModified: String = "Last-Modified"
  const val Link: String = "Link"
  const val Location: String = "Location"
  const val MaxForwards: String = "Max-Forwards"
  const val Origin: String = "Origin"
  const val Pragma: String = "Pragma"
  const val ProxyAuthenticate: String = "Proxy-Authenticate"
  const val ProxyAuthorization: String = "Proxy-Authorization"
  const val Range: String = "Range"
  const val Referer: String = "Referer"
  const val RemoteAddress: String = "Remote-Address"
  const val RetryAfter: String = "Retry-After"
  const val Server: String = "Server"
  const val SetCookie: String = "Set-Cookie"
  const val StrictTransportSecurity: String = "Strict-Transport-Security"
  const val Te: String = "Te"
  const val Trailer: String = "Trailer"
  const val TransferEncoding: String = "Transfer-Encoding"
  const val Upgrade: String = "Upgrade"
  const val UserAgent: String = "User-Agent"
  const val Vary: String = "Vary"
  const val Via: String = "Via"
  const val Warning: String = "Warning"
  const val WwwAuthenticate: String = "WWW-Authenticate"
  const val XFrameOptions: String = "X-Frame-Options"
  const val XForwardedFor: String = "X-Forwarded-For"
  const val XForwardedHost: String = "X-Forwarded-Host"
  const val XForwardedPort: String = "X-Forwarded-Port"
  const val XForwardedProto: String = "X-Forwarded-Proto"
  const val XRealIp: String = "X-Real-Ip"
  const val XRequestedWith: String = "X-Requested-With"
  const val XXSSProtection: String = "X-XSS-Protection"

  val ContentHeaders: Set<String> =
    setOf(ContentLength, ContentType, ContentMd5).map(String::toLowerCase).toSet()

  val SensitiveHeaders: Set<String> =
    setOf(Authorization, Cookie, SetCookie).map(String::toLowerCase).toSet()

  /** Performs a case-insensitive check, whether this header name is content-related.
   */
  fun isContent(headerName: String): Boolean =
    ContentHeaders.contains(headerName.toLowerCase().trim())

  /** Performs a case-insensitive check, whether this header is content-related.
   */
  fun isContent(header: Header): Boolean =
    isContent(header.name)

  /** Performs a case-insensitive check, whether this header name is sensitive.
   */
  fun isSensitive(headerName: String): Boolean =
    isSensitive(headerName, SensitiveHeaders)

  /** Performs a case-insensitive check, whether this header name is sensitive.
   */
  fun isSensitive(headerName: String, sensitiveHeaders: Set<String>): Boolean =
    sensitiveHeaders.map(String::toLowerCase).contains(headerName.toLowerCase().trim())

  /** Performs a case-insensitive check, whether this header is sensitive.
   */
  fun isSensitive(header: Header): Boolean =
    isSensitive(header.name, SensitiveHeaders)

  /** Performs a case-insensitive check, whether this header is sensitive.
   */
  fun isSensitive(header: Header, sensitiveHeaders: Set<String>): Boolean =
    isSensitive(header.name, sensitiveHeaders)
}
