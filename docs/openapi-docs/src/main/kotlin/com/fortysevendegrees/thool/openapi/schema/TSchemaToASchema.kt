package com.fortysevendegrees.thool.openapi.schema

import com.fortysevendegrees.thool.openapi.ExampleValue
import com.fortysevendegrees.thool.openapi.ReferenceOr
import arrow.core.Either
import com.fortysevendegrees.thool.FieldName
import com.fortysevendegrees.thool.Schema

internal fun Schema<*>.toReferenceOrSchema(objectToSchemaReference: ObjectToSchemaReference): ReferenceOr<com.fortysevendegrees.thool.openapi.schema.Schema> =
  when (this) {
    is Schema.Number.UInt -> Either.Right(Schema(type = SchemaType.Integer, format = SchemaFormat.Int32))
    is Schema.Number.Int -> Either.Right(Schema(type = SchemaType.Integer, format = SchemaFormat.Int32))
    is Schema.Number.UByte -> Either.Right(Schema(type = SchemaType.Integer, format = SchemaFormat.Byte))
    is Schema.Number.Byte -> Either.Right(Schema(type = SchemaType.Integer, format = SchemaFormat.Byte))
    is Schema.Number.UShort -> Either.Right(Schema(type = SchemaType.Integer, format = SchemaFormat.Int32))
    is Schema.Number.Short -> Either.Right(Schema(type = SchemaType.Integer, format = SchemaFormat.Int32))
    is Schema.Number.Double -> Either.Right(Schema(type = SchemaType.Number, format = SchemaFormat.Double))
    is Schema.Number.Float -> Either.Right(Schema(type = SchemaType.Number, format = SchemaFormat.Float))
    is Schema.Number.Long -> Either.Right(Schema(type = SchemaType.Number, format = SchemaFormat.Int64))
    is Schema.Number.ULong -> Either.Right(Schema(type = SchemaType.Number, format = SchemaFormat.Int64))
    is Schema.Boolean -> Either.Right(Schema(type = SchemaType.Boolean))
    is Schema.String -> Either.Right(Schema(type = SchemaType.String))
    is Schema.Binary -> Either.Right(Schema(type = SchemaType.String, format = SchemaFormat.Binary))
    is Schema.Date -> Either.Right(Schema(type = SchemaType.String, format = SchemaFormat.Date))
    is Schema.DateTime -> Either.Right(Schema(type = SchemaType.String, format = SchemaFormat.DateTime))

    is Schema.Product -> Either.Right(
      Schema(
        type = SchemaType.Object,
        required = required().map(FieldName::encodedName),
        properties = fields.map { (name, schema) ->
          when (schema) {
            is Schema.Object<*> -> Pair(name.encodedName, Either.Left(objectToSchemaReference.map(schema.objectInfo)))
            else -> Pair(name.encodedName, toReferenceOrSchema(objectToSchemaReference))
          }
        }.toMap()
      )
    )

    is Schema.List -> Either.Right(
      Schema(
        type = SchemaType.Array,
        items = when (val element = this.element) {
          is Schema.Object<*> -> Either.Left(objectToSchemaReference.map(element.objectInfo))
          else -> element.toReferenceOrSchema(objectToSchemaReference)
        }
      )
    )

    is Schema.Coproduct -> Either.Right(
      Schema(
        oneOf = schemas.mapNotNull {
          (it as? Schema.Product)?.objectInfo?.let { info -> Either.Left(objectToSchemaReference.map(info)) }
        },
        //discriminator =
      )
    )

    is Schema.Either -> Either.Right(
      Schema(
        oneOf = listOf(left, right).mapNotNull {
          (it as? Schema.Product)?.objectInfo?.let { info -> Either.Left(objectToSchemaReference.map(info)) }
        }, //discriminator =
      )
    )
    is Schema.Enum -> Either.Right(
      Schema(
        type = SchemaType.String,
        enumeration = values.map(Schema.EnumValue::name)
      )
    )
    is Schema.Nullable -> element.toReferenceOrSchema(objectToSchemaReference)
    is Schema.Map -> Either.Right(
      Schema(
        type = SchemaType.Object,
        required = emptyList(),
        additionalProperties = when (val v = valueSchema) {
          is Schema.Object -> Either.Left(objectToSchemaReference.map(v.objectInfo))
          else -> valueSchema.toReferenceOrSchema(objectToSchemaReference)
        }
      )
    )
    is Schema.OpenProduct -> Either.Right(
      Schema(
        type = SchemaType.Object,
        required = emptyList(),
        additionalProperties = when (val v = valueSchema) {
          is Schema.Object -> Either.Left(objectToSchemaReference.map(v.objectInfo))
          else -> valueSchema.toReferenceOrSchema(objectToSchemaReference)
        }
      )
    )
  }.map { it.addMetadata(this) }

private fun com.fortysevendegrees.thool.openapi.schema.Schema.addMetadata(schema: Schema<*>): com.fortysevendegrees.thool.openapi.schema.Schema =
  copy(
    description = schema.info.description ?: this.description,
    default = schema.info.default?.let { (_, raw) -> raw?.let { ExampleValue(schema, it) } } ?: this.default,
    example = schema.info.encodedExample?.let { ExampleValue(schema, it) } ?: this.example,
    format = schema.info.format ?: this.format,
    deprecated = if (schema.info.deprecated) true else this.deprecated
  )
