package arrow.endpoint.model

internal actual object UriCompatibility {
  // TODO use Punycode for JS, Native libidn https://www.gnu.org/software/libidn/
  public actual fun encodeDNSHost(host: String): String =
    TODO()
}
