package arrow.endpoint.model

internal actual object UriCompatibility {
  // TODO use Punycode for JS, Native libidn https://www.gnu.org/software/libidn/
  public actual fun encodeDNSHost(host: String): String =
    TODO()

  // TODO https://stackoverflow.com/questions/607176/java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-outpu
  public actual fun encodeQuery(s: String, enc: String): String =
    TODO()
}
