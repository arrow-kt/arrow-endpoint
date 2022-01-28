package arrow.endpoint.model

import arrow.endpoint.encode
import arrow.endpoint.model.Rfc3986.encode
import arrow.endpoint.toASCII

internal actual object UriCompatibility {
  actual fun encodeDNSHost(host: String): String =
    toASCII(host).encode(allowedCharacters = Rfc3986.Host)

  actual fun encodeQuery(s: String, enc: String): String =
    encode(s, enc)
}
