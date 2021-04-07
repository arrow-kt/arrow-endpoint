package com.fortysevendegrees.thool.ktor

import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.FieldName
import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.SchemaType.SObject.SProduct
import com.fortysevendegrees.thool.SchemaType.SObjectInfo
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
