package arrow.endpoint

import java.net.URLDecoder
import java.net.URLEncoder

internal actual object UrlencodedData {
  actual fun decode(s: String, charset: String): List<Pair<String, String>> =
    s.split("&").mapNotNull { kv ->
      val res = kv.split(Regex("="), 2)
      when (res.size) {
        2 -> Pair(URLDecoder.decode(res[0], charset), URLDecoder.decode(res[1], charset))
        else -> null
      }
    }

  actual fun encode(s: List<Pair<String, String>>, charset: String): String =
    s.joinToString("&") { (k, v) ->
      "${URLEncoder.encode(k, charset)}=${URLEncoder.encode(v, charset)}"
    }
}
