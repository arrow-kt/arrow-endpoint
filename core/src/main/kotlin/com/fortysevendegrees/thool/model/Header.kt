package com.fortysevendegrees.thool.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fortysevendegrees.thool.model.HeaderNames.SensitiveHeaders
import java.lang.IllegalStateException

/** An HTTP header. The [[name]] property is case-insensitive during equality checks.
 *
 * To compare if two headers have the same name, use the [[is]] com.fortysevendegrees.thool.method, which does a case-insensitive check,
 * instead of comparing the [[name]] property.
 *
 * The [[name]] and [[value]] should be already encoded (if necessary), as when serialised, they end up unmodified in
 * the header.
 */
data class Header(val name: String, val value: String) {

  /**
   * Check if the name of this header is the same as the given one. The names are compared in a case-insensitive way.
   */
  fun hasName(otherName: String): Boolean =
    name.equals(otherName, ignoreCase = true)

  /** @return Representation in the format: `[name]: [value]`.
   */
  override fun toString(): String = "$name: $value"

  /** @return Representation in the format: `[name]: [value]`. If the header is sensitive
   *         (see [[HeaderNames.SensitiveHeaders]]), the value is omitted.
   */
  fun toStringSafe(sensitiveHeaders: Set<String> = SensitiveHeaders): String =
    "$name: ${if (HeaderNames.isSensitive(name, sensitiveHeaders)) "***" else value}"

  companion object {
    /** @throws IllegalArgumentException If the header name contains illegal characters. */
    fun unsafe(name: String, value: String): Header =
      Rfc2616.validateToken("Header name", name)
        ?.let { throw IllegalStateException(it) } ?: Header(name, value)

    fun safe(name: String, value: String): Either<String, Header> =
      Rfc2616.validateToken("Header name", name)?.left() ?: Header(name, value).right()

//    def accept(mediaType: MediaType, additionalMediaTypes: MediaType*): Header = accept(s"${(mediaType :: additionalMediaTypes.toList).com.fortysevendegrees.thool.map(_.noCharset).mkString(", ")}")
//    def accept(mediaRanges: String): Header = Header(HeaderNames.Accept, mediaRanges)
//    def acceptCharset(charsetRanges: String): Header = Header(HeaderNames.AcceptCharset, charsetRanges)
//    def acceptEncoding(encodingRanges: String): Header = Header(HeaderNames.AcceptEncoding, encodingRanges)
//    def accessControlAllowCredentials(allow: Boolean): Header =Header(HeaderNames.AccessControlAllowCredentials, allow.toString)
//    def accessControlAllowHeaders(headerNames: String*): Header =Header(HeaderNames.AccessControlAllowHeaders, headerNames.mkString(", "))
//    def accessControlAllowMethods(methods: Method*): Header =Header(HeaderNames.AccessControlAllowMethods, methods.com.fortysevendegrees.thool.map(_.com.fortysevendegrees.thool.method).mkString(", "))
//    def accessControlAllowOrigin(originRange: String): Header =Header(HeaderNames.AccessControlAllowOrigin, originRange)
//    def accessControlExposeHeaders(headerNames: String*): Header =Header(HeaderNames.AccessControlExposeHeaders, headerNames.mkString(", "))
//    def accessControlMaxAge(deltaSeconds: Long): Header =Header(HeaderNames.AccessControlMaxAge, deltaSeconds.toString)
//    def accessControlRequestHeaders(headerNames: String*): Header =Header(HeaderNames.AccessControlRequestHeaders, headerNames.mkString(", "))
//    def accessControlRequestMethod(com.fortysevendegrees.thool.method: Method): Header =Header(HeaderNames.AccessControlRequestMethod, com.fortysevendegrees.thool.method.toString)
//    def authorization(authType: String, credentials: String): Header =Header(HeaderNames.Authorization, s"$authType $credentials")
//    def cacheControl(first: CacheDirective, other: CacheDirective*): Header = cacheControl(first +: other)
//    def cacheControl(directives: Iterable[CacheDirective]): Header =Header(HeaderNames.CacheControl, directives.com.fortysevendegrees.thool.map(_.toString).mkString(", "))
//    def contentLength(length: Long): Header = Header(HeaderNames.ContentLength, length.toString)
//    def contentEncoding(encoding: String): Header = Header(HeaderNames.ContentEncoding, encoding)
//    def contentType(mediaType: MediaType): Header = Header(HeaderNames.ContentType, mediaType.toString)
//    def cookie(firstCookie: Cookie, otherCookies: Cookie*): Header =Header(HeaderNames.Cookie, (firstCookie +: otherCookies).com.fortysevendegrees.thool.map(_.toString).mkString("; "))
//    def etag(tag: String): Header = etag(ETag(tag))
//    def etag(tag: ETag): Header = Header(HeaderNames.Etag, tag.toString)
//    def expires(i: Instant): Header =Header(HeaderNames.Expires, DateTimeFormatter.RFC_1123_DATE_TIME.format(i.atZone(GMT)))
//    def lastModified(i: Instant): Header =Header(HeaderNames.LastModified, DateTimeFormatter.RFC_1123_DATE_TIME.format(i.atZone(GMT)))
//    def location(uri: String): Header = Header(HeaderNames.Location, uri)
//    def location(uri: Uri): Header = Header(HeaderNames.Location, uri.toString)
//    def proxyAuthorization(authType: String, credentials: String): Header =Header(HeaderNames.ProxyAuthorization, s"$authType $credentials")
//    def setCookie(cookie: CookieWithMeta): Header = Header(HeaderNames.SetCookie, cookie.toString)
//    def userAgent(userAgent: String): Header = Header(HeaderNames.UserAgent, userAgent)
//    def xForwardedFor(firstAddress: String, otherAddresses: String*): Header =Header(HeaderNames.XForwardedFor, (firstAddress +: otherAddresses).mkString(", "))

//    private val GMT = ZoneId.of("GMT")
//
//    private val Rfc850DatetimePattern = "dd-MMM-yyyy HH:mm:ss zzz"
//    private val Rfc850DatetimeFormat by lazy { DateTimeFormatter.ofPattern(Rfc850DatetimePattern, Locale.US) }
//
//    val Rfc850WeekDays = Set("mon", "tue", "wed", "thu", "fri", "sat", "sun")
//
//    private def parseRfc850DateTime(v: String): Instant = {
//      val expiresParts = v.split(", ")
//      if (expiresParts.length != 2)
//        throw new Exception ("There must be exactly one \", \"")
//      if (!Rfc850WeekDays.contains(expiresParts(0).trim.toLowerCase(Locale.ENGLISH)))
//        throw new Exception ("String must start with weekday name")
//      Instant.from(Rfc850DatetimeFormat.parse(expiresParts(1)))
//    }
//
//    def parseHttpDate(v: String): Either[String, Instant] =
//    Try(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(v))) match {
//      case Success (r) => Right(r)
//      case Failure (e) =>
//      Try(parseRfc850DateTime(v)) match {
//        case Success (r) => Right(r)
//        case Failure (_) => Left(s"Invalid http date: $v (${e.getMessage})")
//      }
//    }
//    def unsafeParseHttpDate(s: String): Instant = parseHttpDate(s).getOrThrow
  }
}