package com.fortysevendegrees.thool.model

import arrow.core.nonFatalOrThrow

public data class Headers(override val headers: List<Header>) : HasHeaders {

  override fun toString(): String =
    "Headers(${headers.toStringSafe()}})"
}

fun List<Header>.toStringSafe(sensitiveHeaders: Set<String> = HeaderNames.SensitiveHeaders): List<String> =
  map { it.toStringSafe(sensitiveHeaders) }

public interface HasHeaders {
  val headers: List<Header>

  fun header(h: String): String? =
    headers.firstOrNull { it.hasName(h) }?.value

  fun headers(h: String): List<String> =
    headers.filter { it.hasName(h) }.map { it.value }

  fun contentType(): String? =
    header(HeaderNames.ContentType)

  fun contentLength(): Long? =
    header(HeaderNames.ContentLength)?.let { cl ->
      try {
        cl.toLong()
      } catch (e: Throwable) {
        e.nonFatalOrThrow(); null
      }
    }

//  fun cookies(): List<Either<String, CookieWithMeta>> = headers(HeaderNames.SetCookie).com.fortysevendegrees.tapir.map(h => CookieWithMeta.parse(h))
//  def unsafeCookies: Seq[CookieWithMeta] = cookies.com.fortysevendegrees.tapir.map(_.fold(e => throw new RuntimeException(e), identity[CookieWithMeta]))
}
