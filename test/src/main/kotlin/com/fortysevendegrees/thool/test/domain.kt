package com.fortysevendegrees.thool.test

import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.FieldName
import com.fortysevendegrees.thool.JsonCodec
import com.fortysevendegrees.thool.Schema
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class Test { A, B, C; }

@Serializable
public data class Person(val name: String, val age: Int)

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
