package arrow.endpoint.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.lang.IllegalStateException

/**
 * An HTTP header.
 * The [name] property is case-insensitive during equality checks.
 * To compare if two headers have the same name, use the [hasName] method, which does a case-insensitive check,
 * instead of comparing the [name] property.
 *
 * The [name] and [value] should be already encoded (if necessary), as when serialised, they end up unmodified in
 * the header.
 */
public data class Header(val name: String, val value: String) {

  /**
   * Check if the name of this header is the same as the given one. The names are compared in a case-insensitive way.
   */
  public fun hasName(otherName: String): Boolean =
    name.equals(otherName, ignoreCase = true)

  /** @return Representation in the format: `[name]: [value]`. */
  override fun toString(): String = toStringSafe()

  override fun hashCode(): Int =
    (31 * name.lowercase().hashCode()) + value.hashCode()

  override fun equals(other: Any?): Boolean =
    when (other) {
      is Header -> hasName(other.name) && value == other.value
      else -> false
    }

  /**
   *  @return Representation in the format: `[name]: [value]`.
   *  If the header is sensitive (see [Header.SensitiveHeaders]), the value is omitted.
   */
  public fun toStringSafe(sensitiveHeaders: Set<String> = SensitiveHeaders): String =
    "$name: ${if (isSensitive(name, sensitiveHeaders)) "***" else value}"

  public companion object {
    /** @throws IllegalArgumentException If the header name contains illegal characters. */
    public fun unsafe(name: String, value: String): Header =
      Rfc2616.validateToken("Header name", name)
        ?.let { throw IllegalStateException(it) } ?: Header(name, value)

    public fun of(name: String, value: String): Either<String, Header> =
      Rfc2616.validateToken("Header name", name)?.left() ?: Header(name, value).right()

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
      setOf(ContentLength, ContentType, ContentMd5).map(String::lowercase).toSet()

    public val SensitiveHeaders: Set<String> =
      setOf(Authorization, Cookie, SetCookie).map(String::lowercase).toSet()

    /** Performs a case-insensitive check, whether this header name is content-related.
     */
    public fun isContent(headerName: String): Boolean =
      ContentHeaders.contains(headerName.lowercase().trim())

    /** Performs a case-insensitive check, whether this header is content-related. */
    public fun isContent(header: Header): Boolean =
      isContent(header.name)

    /** Performs a case-insensitive check, whether this header name is sensitive. */
    public fun isSensitive(headerName: String): Boolean =
      isSensitive(headerName, SensitiveHeaders)

    /** Performs a case-insensitive check, whether this header name is sensitive. */
    public fun isSensitive(headerName: String, sensitiveHeaders: Set<String>): Boolean =
      sensitiveHeaders.map(String::lowercase).contains(headerName.lowercase().trim())

    /** Performs a case-insensitive check, whether this header is sensitive. */
    public fun isSensitive(header: Header): Boolean =
      isSensitive(header.name, SensitiveHeaders)

    /** Performs a case-insensitive check, whether this header is sensitive. */
    public fun isSensitive(header: Header, sensitiveHeaders: Set<String>): Boolean =
      isSensitive(header.name, sensitiveHeaders)
  }
}

public fun List<Header>.toStringSafe(sensitiveHeaders: Set<String> = Header.SensitiveHeaders): List<String> =
  map { it.toStringSafe(sensitiveHeaders) }

public fun List<Header>.header(h: String): String? =
  firstOrNull { it.hasName(h) }?.value

public fun List<Header>.headers(h: String): List<String> =
  mapNotNull { if (it.hasName(h)) it.value else null }

public fun List<Header>.contentType(): String? =
  header(Header.ContentType)

public fun List<Header>.contentLength(): Long? =
  header(Header.ContentLength)?.let(String::toLongOrNull)
