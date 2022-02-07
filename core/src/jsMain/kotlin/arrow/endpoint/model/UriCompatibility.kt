package arrow.endpoint.model

import arrow.endpoint.model.Rfc3986.encode
import org.w3c.dom.url.URL

internal actual object UriCompatibility {
  actual fun encodeDNSHost(host: String): String =
    URL("http://$host").host.encode(allowedCharacters = Rfc3986.Host)
}
