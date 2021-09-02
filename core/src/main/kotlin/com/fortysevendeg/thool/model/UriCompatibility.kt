package com.fortysevendeg.thool.model

import com.fortysevendeg.thool.model.Rfc3986.encode
import java.net.URLEncoder

internal object UriCompatibility {
  fun encodeDNSHost(host: String): String =
    // TODO make MPP
    java.net.IDN.toASCII(host).encode(allowedCharacters = Rfc3986.Host)

  fun encodeQuery(s: String, enc: String): String =
    URLEncoder.encode(s, enc)
}
