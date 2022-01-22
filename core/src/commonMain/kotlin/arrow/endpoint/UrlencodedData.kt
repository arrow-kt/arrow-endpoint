package arrow.endpoint

import io.ktor.utils.io.charsets.Charset

internal expect object UrlencodedData {
  fun decode(s: String, charset: Charset): List<Pair<String, String>>
  fun encode(s: List<Pair<String, String>>, charset: Charset): String
}
