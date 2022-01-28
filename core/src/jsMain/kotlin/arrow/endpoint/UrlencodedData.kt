package arrow.endpoint

import io.ktor.utils.io.charsets.Charset

internal actual object UrlencodedData {
  actual fun decode(s: String, charset: Charset): List<Pair<String, String>> =
    s.split("&").mapNotNull { kv ->
      val res = kv.split(Regex("="), 2)
      when (res.size) {
        2 -> Pair(decode(res[0], charset.toString()), decode(res[1], charset.toString()))
        else -> null
      }
    }

  actual fun encode(s: List<Pair<String, String>>, charset: Charset): String =
    s.joinToString("&") { (k, v) ->
      "${encode(k, charset.toString())}=${encode(v, charset.toString())}"
    }
}
