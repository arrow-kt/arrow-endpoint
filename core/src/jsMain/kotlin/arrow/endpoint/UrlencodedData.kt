package arrow.endpoint

import arrow.endpoint.decode as jsDecode

internal actual object UrlencodedData {
  actual fun decode(s: String, charset: String): List<Pair<String, String>> =
    s.split("&").mapNotNull { kv ->
      val res = kv.split(Regex("="), 2)
      when (res.size) {
        2 -> Pair(jsDecode(res[0], charset), jsDecode(res[1], charset))
        else -> null
      }
    }

  actual fun encode(s: List<Pair<String, String>>, charset: String): String = s.joinToString("&") { (k, v) ->
    "${encode(k, charset)}=${encode(v, charset)}"
  }
}
