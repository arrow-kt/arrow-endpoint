package arrow.endpoint

enum class Test { A, B, C; }

public data class Person(val name: String, val age: Int)

fun Schema.Companion.person(): Schema<Person> =
  Schema.Product(
    Schema.ObjectInfo("arrow.endpoint.Person"),
    listOf(
      Pair(FieldName("name"), string),
      Pair(FieldName("age"), int)
    )
  )
