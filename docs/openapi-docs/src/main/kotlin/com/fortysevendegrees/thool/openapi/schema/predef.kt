package com.fortysevendegrees.thool.openapi.schema

import com.fortysevendegrees.thool.Schema


internal typealias ObjectSchema = Schema.ObjectInfo
internal typealias ObjectKey = String

private data class Assigment<A>(val nameToT: Map<String, A>, val tToKey: Map<A, String>)

internal fun <A> Iterable<A>.calculateUniqueKeys(toName: (A) -> String): Map<A, String> =
  fold(Assigment<A>(emptyMap(), emptyMap())) { (nameToT, tToKey), t ->
    val key = uniqueName(toName(t)) { name -> !nameToT.contains(name) || nameToT[name] == t }
    Assigment(
      nameToT + Pair(key, t),
      tToKey + Pair(t, key)
    )
  }.tToKey

internal fun uniqueName(base: String, isUnique: (String) -> Boolean): String {
  var i = 0
  var result = base
  while (!isUnique(result)) {
    i += 1
    result = base + i
  }
  return result
}
