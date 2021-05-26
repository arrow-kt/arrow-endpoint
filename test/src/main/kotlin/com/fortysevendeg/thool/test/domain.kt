package com.fortysevendeg.thool.test

import com.fortysevendeg.thool.Codec
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.FieldName
import com.fortysevendeg.thool.JsonCodec
import com.fortysevendeg.thool.Schema
import com.fortysevendeg.thool.model.CodecFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public enum class Test { A, B, C; }

@Serializable
public data class Person(val name: String, val age: Int)

public fun Codec.Companion.person(): JsonCodec<Person> =
  Codec.json(Schema.person(), { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }

public fun Schema.Companion.person(): Schema<Person> =
  Schema.Product(
    Schema.ObjectInfo("Person"),
    listOf(
      Pair(FieldName("name"), Schema.string),
      Pair(FieldName("age"), Schema.int)
    )
  )

data class Fruit(val name: String)

@Serializable
data class FruitAmount(val fruit: String, val amount: Int)

public fun Codec.Companion.jsonFruitAmount(): JsonCodec<FruitAmount> =
  Codec.json(Schema.fruitAmount(), { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }

public fun Codec.Companion.jsonNullableFruitAmount(): JsonCodec<FruitAmount?> =
  Codec.json(
    Schema.fruitAmount().asNullable(),
    { DecodeResult.Value(Json.decodeFromString(it)) }
  ) { Json.encodeToString(it) }

public fun Codec.Companion.formFruitAmount(): Codec<String, FruitAmount, CodecFormat.XWwwFormUrlencoded> =
  formMapCodecUtf8
    .map(
      { form ->
        FruitAmount(
          requireNotNull(form["fruit"]) { "Fruit not found in form" },
          requireNotNull(form["amount"]) { "Amount not found in form" }.toInt()
        )
      },
      { (fruit, amount) ->
        mapOf(
          "fruit" to fruit,
          "amount" to amount.toString()
        )
      }
    )

public fun Schema.Companion.fruitAmount(): Schema<FruitAmount> =
  Schema.Product(
    Schema.ObjectInfo("FruitAmount"),
    listOf(
      Pair(FieldName("fruit"), Schema.string),
      Pair(FieldName("amount"), Schema.int)
    )
  )
