@file:OptIn(ExperimentalIoApi::class)

package arrow.endpoint

import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.charsets.encode
import io.ktor.utils.io.core.ExperimentalIoApi
import io.ktor.utils.io.core.buildPacket

internal object UrlencodedData {
  fun decode(s: String, charset: Charset): List<Pair<String, String>> =
    charset.newDecoder().let { decoder ->
      s.split("&").mapNotNull { kv ->
        val res = kv.split(Regex("="), 2)
        when (res.size) {
          2 -> {
            val key = buildPacket { append(res[0]) }
            val value = buildPacket { append(res[1]) }
            Pair(decoder.decode(key), decoder.decode(value))
          }
          else -> null
        }
      }
    }

  fun encode(s: List<Pair<String, String>>, charset: Charset): String =
    charset.newEncoder().let { encoder ->
      s.joinToString("&") { (k, v) ->
        "${encoder.encode(k)}=${encoder.encode(v)}"
      }
    }
}
