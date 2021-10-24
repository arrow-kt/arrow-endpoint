package arrow.endpoint.model

import java.net.URI

public fun uri(javaUri: URI): Uri? =
  Uri.parse(javaUri.toString()).orNull()

public fun Uri.toJavaUri(): URI = URI(toString())

public fun Uri.resolveOrNull(other: Uri): Uri? =
  uri(toJavaUri().resolve(other.toJavaUri()))

/** Encodes all reserved characters using [java.net.URLEncoder.encode]. */
public val QuerySegment.Companion.All: Encoding
  get() = {
    UriCompatibility.encodeQuery(it, "UTF-8")
  }

public val HostSegment.Companion.Standard: Encoding
  get() = { s ->
    when {
      s.matches(IpV6Pattern) && s.count { it == ':' } >= 2 -> "[$s]"
      else -> UriCompatibility.encodeDNSHost(s)
    }
  }
