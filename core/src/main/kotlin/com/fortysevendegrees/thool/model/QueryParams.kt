package com.fortysevendegrees.thool.model

data class QueryParams(val ps: List<Pair<String, List<String>>>) {

  fun toMap(): Map<String, String> = toList().toMap()
  fun toMultiMap(): Map<String, List<String>> = ps.toMap()
  fun toList(): List<Pair<String, String>> = ps.flatMap { (k, vs) -> vs.map { Pair(k, it) } }
  fun toMultiList(): List<Pair<String, List<String>>> = ps.toList()

  fun get(s: String): String? =
    getMulti(s)?.firstOrNull()

  fun getMulti(s: String): List<String>? =
    ps.firstOrNull { it.first == s }?.second

  fun param(k: String, v: String): QueryParams = QueryParams(ps + Pair(k, listOf(v)))
  fun param(k: String, v: List<String>): QueryParams = QueryParams(ps + Pair(k, v))
  fun param(p: Map<String, String>): QueryParams = QueryParams(ps + p.map { (k, v) -> Pair(k, listOf(v)) })

}
