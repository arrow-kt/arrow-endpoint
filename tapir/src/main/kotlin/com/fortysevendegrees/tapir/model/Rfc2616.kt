package com.fortysevendegrees.tapir.model

// https://tools.ietf.org/html/rfc2616#page-21
object Rfc2616 {
  val CTL = "\\x00-\\x1F\\x7F"
  val Separators = "()<>@,;:\\\\\"/\\[\\]?={} \\x09"
  private val TokenRegexPart = "[^$Separators$CTL]*"
  val Token: Regex = TokenRegexPart.toRegex()
  val Parameter: Regex = "$TokenRegexPart=$TokenRegexPart".toRegex()

  fun validateToken(componentName: String, v: String): String? =
    if (Token.split(v).isEmpty()) """$componentName can not contain separators: ()<>@,;:\"/[]?={}, or whitespace."""
    else null
}