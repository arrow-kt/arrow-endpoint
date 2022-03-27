package arrow.endpoint.model

import arrow.endpoint.model.Rfc3986.encode
import java.net.URLEncoder

internal object UriCompatibility {
  // TODO use Punycode for JS, Native libidn https://www.gnu.org/software/libidn/
  /* expect */ fun encodeDNSHost(host: String): String =
    java.net.IDN.toASCII(host).encode(allowedCharacters = Rfc3986.Host)

  // TODO
  // https://stackoverflow.com/questions/607176/java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-outpu
  /* expect*/ fun encodeQuery(s: String, enc: String): String = URLEncoder.encode(s, enc)
}
