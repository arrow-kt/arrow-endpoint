package arrow.endpoint.model

internal expect object UriCompatibility {
  // TODO use Punycode for JS, Native libidn https://www.gnu.org/software/libidn/
  fun encodeDNSHost(host: String): String

  // TODO https://stackoverflow.com/questions/607176/java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-outpu
  fun encodeQuery(s: String, enc: String): String
}
