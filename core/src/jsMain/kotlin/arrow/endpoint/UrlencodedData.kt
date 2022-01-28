package arrow.endpoint

import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.name

internal actual object UrlencodedData {
  actual fun decode(s: String, charset: Charset): List<Pair<String, String>> = s.split("&").mapNotNull { kv ->
    val res = kv.split(Regex("="), 2)
    when (res.size) {
      2 -> Pair(decode(res[0], charset.name), decode(res[1], charset.name))
      else -> null
    }
  }

  actual fun encode(s: List<Pair<String, String>>, charset: Charset): String = s.joinToString("&") { (k, v) ->
    "${encode(k, charset.name)}=${encode(v, charset.name)}"
  }
}
