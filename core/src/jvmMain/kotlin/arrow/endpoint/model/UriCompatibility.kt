package arrow.endpoint.model

import arrow.endpoint.model.Rfc3986.encode

internal actual object UriCompatibility {
  actual fun encodeDNSHost(host: String): String =
    java.net.IDN.toASCII(host).encode(allowedCharacters = Rfc3986.Host)
}
