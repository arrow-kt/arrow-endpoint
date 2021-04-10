package com.fortysevendegrees.thool

enum class Test { A, B, C; }

data class Person(val name: String, val age: Int)

fun Schema.Companion.person(): Schema<Person> =
  Schema.Product(
    Schema.ObjectInfo("Person"),
    listOf(
      Pair(FieldName("name"), Schema.string),
      Pair(FieldName("age"), Schema.int)
    )
  )
