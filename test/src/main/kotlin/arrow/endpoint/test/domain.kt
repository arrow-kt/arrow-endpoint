package arrow.endpoint.test

import arrow.endpoint.Codec
import arrow.endpoint.DecodeResult
import arrow.endpoint.FieldName
import arrow.endpoint.JsonCodec
import arrow.endpoint.Schema
import arrow.endpoint.model.CodecFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public enum class Test {
  A,
  B,
  C
}

@Serializable public data class Person(val name: String, val age: Int)

@OptIn(ExperimentalSerializationApi::class)
public fun Codec.Companion.person(): JsonCodec<Person> =
  json(Schema.person(), { DecodeResult.Value(Json.decodeFromString(it)) }) {
    Json.encodeToString(it)
  }

public fun Schema.Companion.person(): Schema<Person> =
  Schema.Product(
    Schema.ObjectInfo("Person"),
    listOf(Pair(FieldName("name"), string), Pair(FieldName("age"), int))
  )

public data class Fruit(val name: String)

@Serializable public data class FruitAmount(val fruit: String, val amount: Int)

@OptIn(ExperimentalSerializationApi::class)
public fun Codec.Companion.jsonFruitAmount(): JsonCodec<FruitAmount> =
  json(Schema.fruitAmount(), { DecodeResult.Value(Json.decodeFromString(it)) }) {
    Json.encodeToString(it)
  }

@OptIn(ExperimentalSerializationApi::class)
public fun Codec.Companion.jsonNullableFruitAmount(): JsonCodec<FruitAmount?> =
  json(Schema.fruitAmount().asNullable(), { DecodeResult.Value(Json.decodeFromString(it)) }) {
    Json.encodeToString(it)
  }

public fun Codec.Companion.formFruitAmount():
  Codec<String, FruitAmount, CodecFormat.XWwwFormUrlencoded> =
  formMapCodecUtf8.map(
    { form ->
      FruitAmount(
        requireNotNull(form["fruit"]) { "Fruit not found in form" },
        requireNotNull(form["amount"]) { "Amount not found in form" }.toInt()
      )
    },
    { (fruit, amount) -> mapOf("fruit" to fruit, "amount" to amount.toString()) }
  )

public fun Schema.Companion.fruitAmount(): Schema<FruitAmount> =
  Schema.Product(
    Schema.ObjectInfo("FruitAmount"),
    listOf(Pair(FieldName("fruit"), string), Pair(FieldName("amount"), int))
  )
