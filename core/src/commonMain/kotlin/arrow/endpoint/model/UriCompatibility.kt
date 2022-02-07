package arrow.endpoint.model

import arrow.endpoint.model.Rfc3986.Host
import arrow.endpoint.model.Rfc3986.encode
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.encode
import io.ktor.utils.io.core.forEach

internal expect object UriCompatibility {
  fun encodeDNSHost(host: String): String //=
    //host.encode(Host)
}

private val URL_ALPHABET: List<Byte> = (('a'..'z') + ('A'..'Z') + ('0'..'9')).map { it.code.toByte() }

/**
 * https://tools.ietf.org/html/rfc3986#section-2
 */
internal val URL_PROTOCOL_PART = listOf(
  ':', '/', '?', '#', '[', ']', '@', // general
  '!', '$', '&', '\'', '(', ')', '*', ',', ';', '=', // sub-components
  '-', '.', '_', '~', '+' // unreserved
).map { it.code.toByte() }


private fun Byte.percentEncode(): String = buildString(3) {
  val code = toInt() and 0xff
  append('%')
  append(hexDigitToChar(code shr 4))
  append(hexDigitToChar(code and 0x0f))
}

private fun charToHexDigit(c2: Char) = when (c2) {
  in '0'..'9' -> c2 - '0'
  in 'A'..'F' -> c2 - 'A' + 10
  in 'a'..'f' -> c2 - 'a' + 10
  else -> -1
}

private fun hexDigitToChar(digit: Int): Char = when (digit) {
  in 0..9 -> '0' + digit
  else -> 'A' + digit - 10
}

@Suppress("EXPERIMENTAL_API_USAGE")
internal fun String.encodeURLQueryComponent(
  encodeFull: Boolean = false,
  spaceToPlus: Boolean = false,
  charset: Charset = Charsets.UTF_8
): String = buildString {
  val content = charset.newEncoder().encode(this@encodeURLQueryComponent)
  content.forEach {
    when {
      it == ' '.code.toByte() -> if (spaceToPlus) append('+') else append("%20")
      it in URL_ALPHABET || (!encodeFull && it in URL_PROTOCOL_PART) -> append(it.toChar())
      else -> append(it.percentEncode())
    }
  }
}
