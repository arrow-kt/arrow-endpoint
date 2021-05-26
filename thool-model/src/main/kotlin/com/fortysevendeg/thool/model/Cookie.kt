package com.fortysevendeg.thool.model

import arrow.core.Either
import arrow.core.Nullable
import arrow.core.left
import arrow.core.right
import arrow.core.sequenceEither

/**
 * A cookie name-value pair.
 * The `name` and `value` should be already encoded (if necessary), as when serialised, they end up unmodified in the header.
 */
public data class Cookie(val name: String, val value: String) {

  /**
   * @return Representation of the cookie as in a header value, in the format: `[name]=[value]`.
   */
  override fun toString(): String = "$name=$value"

  public companion object {
    // see: https://stackoverflow.com/questions/1969232/allowed-characters-in-cookies/1969339
    private val AllowedValueCharacters = "[^${Rfc2616.CTL}]*".toRegex()

    private fun validateName(name: String): String? =
      Rfc2616.validateToken("Cookie name", name)

    private fun validateValue(value: String): String? =
      if (AllowedValueCharacters.matches(value)) null
      else "Cookie value can not contain control characters"

    public fun of(name: String, value: String): Either<String, Cookie> =
      Nullable.zip(validateName(name), validateValue(value)) { a, b -> "$a, $b" }?.left() ?: Cookie(name, value).right()

    /**
     * Parse the cookie, represented as a header value (in the format: `[name]=[value]`).
     */
    public fun parse(s: String): Either<String, List<Cookie>> {
      val cs = s.split(";").map { ss ->
        val cookie = ss.split(Regex("="), limit = 2).map(String::trim)
        when (cookie.size) {
          1 -> Cookie.of(cookie[0], "")
          2 -> Cookie.of(cookie[0], cookie[1])
          else -> throw RuntimeException("Cookie.parse failed. Expected size 1 or 2 but found $cookie")
        }
      }

      return cs.sequenceEither()
    }
  }
}
