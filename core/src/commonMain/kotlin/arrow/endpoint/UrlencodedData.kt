package arrow.endpoint

internal expect object UrlencodedData {
  fun decode(s: String, charset: String): List<Pair<String, String>>
  fun encode(s: List<Pair<String, String>>, charset: String): String
}
