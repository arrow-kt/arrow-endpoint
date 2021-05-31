package com.fortysevendegrees.thool.docs.openapi

import arrow.core.Either
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.EndpointOutput
import com.fortysevendeg.thool.Schema
import com.fortysevendeg.thool.model.StatusCode

private data class Assignment<A>(val nameToT: Map<String, A>, val tToKey: Map<A, String>)

internal fun <A> Iterable<A>.calculateUniqueKeys(toName: (A) -> String): Map<A, String> =
  fold(Assignment<A>(linkedMapOf(), linkedMapOf())) { (nameToT, tToKey), t ->
    val key = uniqueName(toName(t)) { name -> !nameToT.contains(name) || nameToT[name] == t }
    Assignment(
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

/**
 * Keeps only the first object data for each `SObjectInfo`. In case of recursive objects, the first one is the
 * most complete as it contains the built-up structure, unlike subsequent ones, which only represent leaves (Tapir#354).
 */
internal fun Iterable<Pair<Schema.ObjectInfo, Schema<*>>>.unique(): List<Pair<Schema.ObjectInfo, Schema<*>>> {
  val seen = mutableSetOf<Schema.ObjectInfo>()
  val result = mutableListOf<Pair<Schema.ObjectInfo, Schema<*>>>()
  forEach { obj ->
    if (!seen.contains(obj.first)) {
      seen.add(obj.first)
      result += obj
    }
  }
  return result
}

// Outputs may differ basing on status code because of `oneOf`. This method extracts the status code
// mapping to the top-level. In the map, the `None` key stands for the default status code, and a `Some` value
// to the status code specified using `statusMapping` or `statusCode(_)`. Any empty outputs are skipped.
internal typealias BasicOutputs = List<EndpointOutput.Basic<*, *, *>>

internal fun <I> EndpointOutput<I>.asBasicOutputsList(): List<Pair<StatusCode?, BasicOutputs>> =
  when (val res = asBasicOutputsOrList()) {
    is Either.Left -> listOf(Pair(null, res.value))
    is Either.Right -> res.value
  }

private typealias BasicOutputsOrList = Either<BasicOutputs, List<Pair<StatusCode?, BasicOutputs>>>

private fun <I> EndpointOutput<I>.asBasicOutputsOrList(): BasicOutputsOrList {
  fun <A> throwMultipleOneOfMappings(): A = throw IllegalArgumentException("Multiple one-of mappings in output $this")

  fun mergeMultiple(v: List<BasicOutputsOrList>): BasicOutputsOrList =
    v.fold(Either.Left(emptyList())) { acc, os ->
      when (acc) {
        is Either.Left -> when (os) {
          is Either.Left -> Either.Left(acc.value + os.value)
          is Either.Right -> Either.Right(os.value.map { (code, o) -> Pair(code, acc.value + o) })
        }
        is Either.Right -> when (os) {
          is Either.Left -> Either.Right(acc.value.map { (code, o) -> Pair(code, o + os.value) })
          is Either.Right -> throwMultipleOneOfMappings()
        }
      }
    }

  return when (this) {
    is EndpointOutput.Pair<*, *, *> -> mergeMultiple(listOf(this.first.asBasicOutputsOrList(), this.second.asBasicOutputsOrList()))
    is EndpointIO.Pair<*, *, *> -> mergeMultiple(listOf(this.first.asBasicOutputsOrList(), this.second.asBasicOutputsOrList()))
    is EndpointOutput.MappedPair<*, *, *, *> -> this.output.asBasicOutputsOrList()
    is EndpointIO.MappedPair<*, *, *, *> -> this.wrapped.asBasicOutputsOrList()
    is EndpointOutput.Void -> Either.Left(emptyList())
    is EndpointOutput.FixedStatusCode -> Either.Right(listOf(this.statusCode to listOf(this)))
    is EndpointOutput.Basic<*, *, *> -> Either.Left(listOf(this))
    is EndpointOutput.StatusCode -> if (documentedCodes.isNotEmpty()) {
      val entries = documentedCodes.keys.map { code -> Pair(code, listOf(this)) }
      Either.Right(entries)
    } else Either.Left(emptyList())
    is EndpointIO.Empty -> Either.Left(emptyList())
  }
}
