package arrow.endpoint.model

public value class QueryParams(internal val ps: List<Pair<String, List<String>>>) {

  public constructor(map: Map<String, String>) : this(map.entries.map { (k, v) -> Pair(k, listOf(v)) })

  public fun all(): List<Pair<String, List<String>>> = ps
  public fun toMap(): Map<String, String> = toList().toMap()
  public fun toMultiMap(): Map<String, List<String>> = ps.toMap()
  public fun toList(): List<Pair<String, String>> = ps.flatMap { (k, vs) -> vs.map { Pair(k, it) } }
  public fun toMultiList(): List<Pair<String, List<String>>> = ps.toList()

  public operator fun get(s: String): String? = getMulti(s)?.firstOrNull()

  public fun getMulti(s: String): List<String>? =
    ps.firstOrNull { it.first == s }?.second

  public fun param(k: String, v: String): QueryParams = QueryParams(ps + Pair(k, listOf(v)))
  public fun param(k: String, v: List<String>): QueryParams = QueryParams(ps + Pair(k, v))
  public fun param(p: Map<String, String>): QueryParams = QueryParams(ps + p.map { (k, v) -> Pair(k, listOf(v)) })
}
