package com.fortysevendegrees.thool.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fortysevendegrees.thool.model.Headers.Companion.SensitiveHeaders
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

  override fun equals(other: Any?): Boolean =
    when (other) {
      is Header -> hasName(other.name) && value == other.value
      else -> false
    }

  /**
   *  @return Representation in the format: `[name]: [value]`.
   *  If the header is sensitive (see [Headers.SensitiveHeaders]), the value is omitted.
   */
  public fun toStringSafe(sensitiveHeaders: Set<String> = SensitiveHeaders): String =
    "$name: ${if (Headers.isSensitive(name, sensitiveHeaders)) "***" else value}"

  public companion object {
    /** @throws IllegalArgumentException If the header name contains illegal characters. */
    public fun unsafe(name: String, value: String): Header =
      Rfc2616.validateToken("Header name", name)
        ?.let { throw IllegalStateException(it) } ?: Header(name, value)

    public fun of(name: String, value: String): Either<String, Header> =
      Rfc2616.validateToken("Header name", name)?.left() ?: Header(name, value).right()
  }
}
