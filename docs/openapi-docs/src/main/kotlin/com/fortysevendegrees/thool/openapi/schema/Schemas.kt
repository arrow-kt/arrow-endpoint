package com.fortysevendegrees.thool.openapi.schema

import com.fortysevendegrees.thool.openapi.ReferenceOr
import arrow.core.Either
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Schema

class Schemas(val objectToSchemaReference: ObjectToSchemaReference) {
  operator fun <T> invoke(codec: Codec<T, *, *>): ReferenceOr<com.fortysevendegrees.thool.openapi.schema.Schema> =
    invoke(codec.schema())

  operator fun invoke(schema: Schema<*>): ReferenceOr<com.fortysevendegrees.thool.openapi.schema.Schema> =
    when (schema) {
      is Schema.List -> when (val element = schema.element) {
        is Schema.Object -> Either.Right(
          Schema(
            type = SchemaType.Array,
            items = Either.Left(objectToSchemaReference.map(element.objectInfo))
          )
        )
        else -> schema.toReferenceOrSchema(objectToSchemaReference)
      }

      is Schema.Object -> Either.Left(objectToSchemaReference.map(schema.objectInfo))
      else -> schema.toReferenceOrSchema(objectToSchemaReference)
    }
}

