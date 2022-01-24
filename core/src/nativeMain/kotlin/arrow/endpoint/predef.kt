package arrow.endpoint

import io.ktor.utils.io.charsets.Charset

internal actual object UrlencodedData {
  actual fun decode(s: String, charset: Charset): List<Pair<String, String>> =
    TODO()

  actual fun encode(s: List<Pair<String, String>>, charset: Charset): String =
    TODO()
}
