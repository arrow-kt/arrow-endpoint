package com.fortysevendegrees.tapir.ktor

import com.fortysevendegrees.tapir.Codec
import com.fortysevendegrees.tapir.DecodeResult
import com.fortysevendegrees.tapir.FieldName
import com.fortysevendegrees.tapir.Schema
import com.fortysevendegrees.tapir.SchemaType.SObject.SProduct
import com.fortysevendegrees.tapir.SchemaType.SObjectInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Project(
  val name: String,
  val language: String
) {
  companion object {
    val schema: Schema<Project> = Schema(
      SProduct(
        SObjectInfo("Project"),
        listOf(
          Pair(FieldName("name"), Schema.string),
          Pair(
            FieldName("age"), Schema.int.description("test").default(10)
          )
        )
      )
    )

    val jsonCodec = Codec.json(schema, { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }
  }
}
