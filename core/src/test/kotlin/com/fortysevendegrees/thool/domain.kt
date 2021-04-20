package com.fortysevendegrees.thool

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class Test { A, B, C; }

@Serializable
data class Person(val name: String, val age: Int)

fun Codec.Companion.person(): JsonCodec<Person> =
  Codec.json(Schema.person(), { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }

fun Schema.Companion.person(): Schema<Person> =
  Schema.Product(
    Schema.ObjectInfo("Person"),
    listOf(
      Pair(FieldName("name"), Schema.string),
      Pair(FieldName("age"), Schema.int)
    )
  )
