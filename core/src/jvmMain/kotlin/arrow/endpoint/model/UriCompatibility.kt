package arrow.endpoint.model

import arrow.endpoint.model.Rfc3986.encode
import java.net.URLEncoder

internal actual object UriCompatibility {
  actual fun encodeDNSHost(host: String): String =
    java.net.IDN.toASCII(host).let {
      it.encode(allowedCharacters = Rfc3986.Host)
    }

  actual fun encodeQuery(s: String, enc: String): String =
    URLEncoder.encode(s, enc)
}
