package com.fortysevendegrees.thool.model

// https://tools.ietf.org/html/rfc2616#page-21
internal object Rfc2616 {
  const val CTL: String = "\\x00-\\x1F\\x7F"
  const val Separators: String = "()<>@,;:\\\\\"/\\[\\]?={} \\x09"
  private const val TokenRegexPart: String = "[^$Separators$CTL]*"
  val Token: Regex = TokenRegexPart.toRegex()
  val Parameter: Regex = "$TokenRegexPart=$TokenRegexPart".toRegex()

  fun validateToken(componentName: String, v: String): String? =
    if (Token.split(v).isEmpty()) """$componentName can not contain separators: ()<>@,;:\"/[]?={}, or whitespace."""
    else null
}
