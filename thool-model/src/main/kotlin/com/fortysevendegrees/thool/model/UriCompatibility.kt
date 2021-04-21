package com.fortysevendegrees.thool.model

import com.fortysevendegrees.thool.model.Rfc3986.encode
import java.net.URLEncoder

internal object UriCompatibility {
  fun encodeDNSHost(host: String): String =
    java.net.IDN.toASCII(host).encode(allowedCharacters = Rfc3986.Host)

  fun encodeQuery(s: String, enc: String): String =
    URLEncoder.encode(s, enc)
}
