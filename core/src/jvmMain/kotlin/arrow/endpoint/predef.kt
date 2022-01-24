package arrow.endpoint

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset

internal actual object UrlencodedData {
  actual fun decode(s: String, charset: Charset): List<Pair<String, String>> =
    s.split("&").mapNotNull { kv ->
      val res = kv.split(Regex("="), 2)
      when (res.size) {
        2 -> Pair(URLDecoder.decode(res[0], charset.toString()), URLDecoder.decode(res[1], charset.toString()))
        else -> null
      }
    }

  actual fun encode(s: List<Pair<String, String>>, charset: Charset): String =
    s.joinToString("&") { (k, v) ->
      "${URLEncoder.encode(k, charset.toString())}=${URLEncoder.encode(v, charset.toString())}"
    }
}
