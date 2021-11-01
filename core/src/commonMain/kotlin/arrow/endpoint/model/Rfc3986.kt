package arrow.endpoint.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray

internal object Rfc3986 {
  private val AlphaNum: Set<Char> = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toSet()
  private val Unreserved: Set<Char> = AlphaNum + setOf('-', '.', '_', '~')
  private val SubDelims: Set<Char> = setOf('!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=')
  private val PChar: Set<Char> = Unreserved + SubDelims + setOf(':', '@')

  val Scheme: Set<Char> = AlphaNum + setOf('+', '-', '.')
  val UserInfo: Set<Char> = Unreserved + SubDelims
  val Host: Set<Char> = Unreserved + SubDelims
  val PathSegment: Set<Char> = PChar
  val Query: Set<Char> = PChar + setOf('/', '?')
  val Fragment: Set<Char> = Query

  val SegmentWithBrackets: Set<Char> = Query + setOf('[', ']')

  /** @param spaceAsPlus In the query, space is encoded as a `+`. In other
   * contexts, it should be %-encoded as `%20`.
   * @param encodePlus Should `+` (which is the encoded form of space
   * in the query) be %-encoded.
   */
  fun String.encode(
    allowedCharacters: Set<Char>,
    spaceAsPlus: Boolean = false,
    encodePlus: Boolean = false
  ): String =
    buildString {
      // based on https://gist.github.com/teigen/5865923
      for (b in toByteArray(Charsets.UTF_8)) {
        val c = (b.toInt() and 0xff).toChar()
        when {
          c == '+' && encodePlus -> append("%2B")
          allowedCharacters.contains(c) -> append(c)
          c == ' ' && spaceAsPlus -> append('+')
          else -> {
            append("%")
            append(b.format())
          }
        }
      }
    }

  fun String.decode(plusAsSpace: Boolean = false, enc: Charset = Charsets.UTF_8): Either<UriError, String> {
    // Copied from URLDecoder.decode with additional + handling (first case)
    var needToChange = false
    val numChars = length
    val sb = StringBuilder(if (numChars > 500) numChars / 2 else numChars)
    var i = 0

    var c: Char
    var bytes: ByteArray? = null
    while (i < numChars) {
      c = elementAt(i)
      when {
        c == '+' && plusAsSpace -> {
          sb.append(' ')
          i += 1
          needToChange = true
        }
        c == '%' -> {
          /*
           * Starting with this instance of %, process all
           * consecutive substrings of the form %xy. Each
           * substring %xy will yield a byte. Convert all
           * consecutive  bytes obtained this way to whatever
           * character(s) they represent in the provided
           * encoding.
           */
          // (numChars-i)/3 is an upper bound for the number
          // of remaining bytes
          if (bytes == null) bytes = ByteArray((numChars - i) / 3)
          var pos = 0
          while (((i + 2) < numChars) && (c == '%')) {
            val v = try {
              substring(i + 1, i + 3).toInt(16)
            } catch (e: NumberFormatException) {
              return UriError.IllegalArgument("URLDecoder: Illegal hex characters in escape (%) pattern - " + e.message)
                .left()
            }
            if (v < 0)
              return UriError.IllegalArgument("URLDecoder: Illegal hex characters in escape (%) pattern - negative value")
                .left()
            bytes[pos] = v.toByte()
            pos += 1
            i += 3
            if (i < numChars) c = elementAt(i)
          }
          // A trailing, incomplete byte encoding such as
          // "%x" will cause an exception to be thrown
          if ((i < numChars) && (c == '%'))
            return UriError.IllegalArgument("URLDecoder: Incomplete trailing escape (%) pattern").left()
          sb.append(bytes.joinToString { it.toString(16) }, startIndex = 0, endIndex = pos)
          needToChange = true
        }
        else -> {
          sb.append(c)
          i += 1
        }
      }
    }
    return (if (needToChange) sb.toString() else this).right()
  }

  private val hexArray: CharArray
    get() = "0123456789ABCDEF".toCharArray()

  private fun Byte.format(): String {
    val v = toInt().and(0xFF)
    val a = hexArray[v ushr 4]
    val b = hexArray[v and 0x0F]
    return "$a$b"
  }
}
