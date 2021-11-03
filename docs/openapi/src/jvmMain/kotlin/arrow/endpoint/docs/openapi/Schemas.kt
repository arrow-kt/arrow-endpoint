package arrow.endpoint.docs.openapi

import arrow.endpoint.Codec
import arrow.endpoint.FieldName
import arrow.endpoint.Schema as TSchema

internal fun Map<TSchema.ObjectInfo, String>.getReference(
  objectInfo: TSchema.ObjectInfo
): Reference =
  Reference(
    "#/components/schemas/",
    this[objectInfo] ?: throw NoSuchElementException("key not found: $objectInfo")
  )

internal fun Map<TSchema.ObjectInfo, String>.referenceOr(
  codec: Codec<*, *, *>
): Referenced<Schema> = referenceOr(codec.schema())

internal fun Map<TSchema.ObjectInfo, String>.referenceOr(schema: TSchema<*>): Referenced<Schema> =
  when (schema) {
    is TSchema.List ->
      when (val element = schema.element) {
        is TSchema.Object ->
          Referenced.Other(
            Schema(
              type = OpenApiType.array,
              items = Referenced.Ref(getReference(element.objectInfo))
            )
          )
        else -> _referenceOrSchema(schema)
      }
    is TSchema.Object -> Referenced.Ref(getReference(schema.objectInfo))
    else -> _referenceOrSchema(schema)
  }

@Suppress("FunctionName")
internal fun Map<TSchema.ObjectInfo, String>._referenceOrSchema(
  schema: TSchema<*>
): Referenced<Schema> =
  when (schema) {
    is TSchema.Number.UInt ->
      Referenced.Other(Schema(type = OpenApiType.integer, format = Format.int32))
    is TSchema.Number.Int ->
      Referenced.Other(Schema(type = OpenApiType.integer, format = Format.int32))
    is TSchema.Number.UByte ->
      Referenced.Other(Schema(type = OpenApiType.integer, format = Format.byte))
    is TSchema.Number.Byte ->
      Referenced.Other(Schema(type = OpenApiType.integer, format = Format.byte))
    is TSchema.Number.UShort ->
      Referenced.Other(Schema(type = OpenApiType.integer, format = Format.int32))
    is TSchema.Number.Short ->
      Referenced.Other(Schema(type = OpenApiType.integer, format = Format.int32))
    is TSchema.Number.Double ->
      Referenced.Other(Schema(type = OpenApiType.number, format = Format.double))
    is TSchema.Number.Float ->
      Referenced.Other(Schema(type = OpenApiType.number, format = Format.float))
    is TSchema.Number.Long ->
      Referenced.Other(Schema(type = OpenApiType.number, format = Format.int64))
    is TSchema.Number.ULong ->
      Referenced.Other(Schema(type = OpenApiType.number, format = Format.int64))
    is TSchema.Boolean -> Referenced.Other(Schema(type = OpenApiType.boolean))
    is TSchema.String -> Referenced.Other(Schema(type = OpenApiType.string))
    is TSchema.Binary -> Referenced.Other(Schema(type = OpenApiType.string, format = Format.binary))
    is TSchema.Date -> Referenced.Other(Schema(type = OpenApiType.string, format = Format.date))
    is TSchema.DateTime ->
      Referenced.Other(Schema(type = OpenApiType.string, format = Format.datetime))
    is TSchema.Product ->
      Referenced.Other(
        Schema(
          type = OpenApiType.`object`,
          required = schema.required().map(FieldName::encodedName),
          properties =
            schema.fields.associateTo(linkedMapOf()) { (name, schema) ->
              when (schema) {
                is TSchema.Object<*> ->
                  Pair(name.encodedName, Referenced.Ref(getReference(schema.objectInfo)))
                else -> Pair(name.encodedName, _referenceOrSchema(schema))
              }
            }
        )
      )
    is TSchema.List ->
      Referenced.Other(
        Schema(
          type = OpenApiType.array,
          items =
            when (val element = schema.element) {
              is TSchema.Object<*> -> Referenced.Ref(getReference(element.objectInfo))
              else -> _referenceOrSchema(element)
            }
        )
      )
    is TSchema.Coproduct ->
      Referenced.Other(
        Schema(
          oneOf =
            schema.schemas.mapNotNull {
              (it as? TSchema.Product)?.objectInfo?.let { info ->
                Referenced.Ref(getReference(info))
              }
            },
          // discriminator =
          )
      )
    is TSchema.Either ->
      Referenced.Other(
        Schema(
          oneOf =
            listOf(schema.left, schema.right).mapNotNull {
              (it as? TSchema.Product)?.objectInfo?.let { info ->
                Referenced.Ref(getReference(info))
              }
            }, // discriminator =
        )
      )
    is TSchema.Enum ->
      Referenced.Other(
        Schema(type = OpenApiType.string, enum = schema.values.map(TSchema.EnumValue::name))
      )
    is TSchema.Nullable -> _referenceOrSchema(schema.element)
    is TSchema.Map ->
      Referenced.Other(
        Schema(
          type = OpenApiType.`object`,
          required = emptyList(),
          additionalProperties =
            when (val v = schema.valueSchema) {
              is TSchema.Object -> Referenced.Ref(getReference(v.objectInfo))
              else -> _referenceOrSchema(schema.valueSchema)
            }.let { AdditionalProperties.PSchema(it) }
        )
      )
    is TSchema.OpenProduct ->
      Referenced.Other(
        Schema(
          type = OpenApiType.`object`,
          required = emptyList(),
          additionalProperties =
            when (val v = schema.valueSchema) {
              is TSchema.Object -> Referenced.Ref(getReference(v.objectInfo))
              else -> _referenceOrSchema(schema.valueSchema)
            }.let { AdditionalProperties.PSchema(it) }
        )
      )
  }.map { it.addMetadata(schema) }

private fun Schema.addMetadata(schema: TSchema<*>): Schema =
  copy(
    description = schema.info.description ?: this.description,
    default = schema.info.default?.let { (_, raw) -> raw?.let { ExampleValue(schema, it) } }
        ?: this.default,
    example = schema.info.encodedExample?.let { ExampleValue(schema, it) } ?: this.example,
    format = schema.info.format ?: this.format,
    deprecated = if (schema.info.deprecated) true else this.deprecated
  )
