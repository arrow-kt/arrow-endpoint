package com.fortysevendegrees.thool.openapi.schema

import com.fortysevendegrees.thool.openapi.ExampleValue
import com.fortysevendegrees.thool.openapi.ReferenceOr

// todo: discriminator, xml, json-schema properties
data class Schema(
  val allOf: List<ReferenceOr<Schema>> = emptyList(),
  val title: String? = null,
  val required: List<String> = emptyList(),
  val type: SchemaType? = null,
  val items: ReferenceOr<Schema>? = null,
  val properties: Map<String, ReferenceOr<Schema>> = emptyMap(),
  val description: String? = null,
  val format: String? = null,
  val default: ExampleValue? = null,
  val nullable: Boolean? = null,
  val readOnly: Boolean? = null,
  val writeOnly: Boolean? = null,
  val example: ExampleValue? = null,
  val deprecated: Boolean? = null,
  val oneOf: List<ReferenceOr<Schema>> = emptyList(),
  val discriminator: Discriminator? = null,
  val additionalProperties: ReferenceOr<Schema>? = null,
  val enumeration: List<String>? = null
)

data class Discriminator(val propertyName: String, val mapping: Map<String, String>?)

enum class SchemaType { Boolean, Object, Array, Number, String, Integer; }

object SchemaFormat {
  val Int32: String = "int32"
  val Int64: String = "int64"
  val Float: String = "float"
  val Double: String = "double"
  val Byte: String = "byte"
  val Binary: String = "binary"
  val Date: String = "date"
  val DateTime: String = "date-time"
  val Password: String = "password"
}
