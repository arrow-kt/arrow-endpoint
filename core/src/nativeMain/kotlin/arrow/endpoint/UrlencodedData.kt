package arrow.endpoint

internal actual object UrlencodedData {
  actual fun decode(s: String, charset: String): List<Pair<String, String>> =
    TODO()

  actual fun encode(s: List<Pair<String, String>>, charset: String): String =
    TODO()
}
