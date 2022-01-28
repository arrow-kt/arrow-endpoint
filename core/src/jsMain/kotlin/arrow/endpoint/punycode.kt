package arrow.endpoint

@Suppress("ClassName")
@JsModule("punycode")
@JsNonModule
internal external object punycode {
  /** current Punycode.js version number **/
  val version: String

  /**
   * Converts a Punycode string representing a domain name or an email address
   * to Unicode. Only the Punycoded parts of the input will be converted, i.e.
   * it doesn't matter if you call it on a string that has already been
   * converted to Unicode.
   */
  fun toUnicode(input: String): String

  /**
   * Converts a Unicode string representing a domain name or an email address to
   * Punycode. Only the non-ASCII parts of the domain name will be converted,
   * i.e. it doesn't matter if you call it with a domain that's already in
   * ASCII.
   */
  fun toASCII(input: String): String

  /**
   * Converts a string of Unicode symbols (e.g. a domain name label)
   * to a Punycode string of ASCII-only symbols.
   */
  fun encode(input: String): String
}
