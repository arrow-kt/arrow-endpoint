package com.fortysevendegrees.tapir

sealed class SchemaType {
  abstract fun show(): String

  object SString : SchemaType() {
    override fun show(): String = "string"
  }

  object SInteger : SchemaType() {
    override fun show(): String = "integer"
  }

  object SNumber : SchemaType() {
    override fun show(): String = "number"
  }

  object SBoolean : SchemaType() {
    override fun show(): String = "boolean"
  }

  data class SArray(val element: Schema<*>) : SchemaType() {
    override fun show(): String = "array(${element.show()})"
  }

  object SBinary : SchemaType() {
    override fun show(): String = "binary"
  }

  object SDate : SchemaType() {
    override fun show(): String = "date"
  }

  object SDateTime : SchemaType() {
    override fun show(): String = "date-time"
  }

  /* sealed interface */
  sealed class SObject : SchemaType() {
    abstract val info: SObjectInfo

    data class SOpenProduct(override val info: SObjectInfo, val valueSchema: Schema<*>) : SObject() {
      override fun show(): String = "map"
    }

    data class SProduct(override val info: SObjectInfo, val fields: List<Pair<FieldName, Schema<*>>>) : SObject() {
      fun required(): List<FieldName> =
        fields.mapNotNull { (f, s) -> if (!s.isOptional) f else null }

      override fun show(): String = "object(${fields.joinToString(",") { (f, s) -> "$f->${s.show()}" }}"

      companion object {
        val Empty = SProduct(SObjectInfo.unit, emptyList())
      }
    }

    data class SCoproduct(
      override val info: SObjectInfo,
      val schemas: List<Schema<*>>,
      val discriminator: Discriminator?
    ) : SObject() {
      override fun show(): String = "oneOf:" + schemas.joinToString(",")

      fun <D> addDiscriminatorField(
        discriminatorName: FieldName,
        discriminatorSchema: Schema<D> = Schema.string(),
        discriminatorMappingOverride: Map<String, SRef> = emptyMap()
      ): SCoproduct =
        SCoproduct(
          info,
          schemas.map { schema ->
            when (schema.schemaType) {
              is SProduct ->
                schema.copy(
                  schemaType = schema.schemaType.copy(
                    fields = schema.schemaType.fields + Pair(
                      discriminatorName,
                      discriminatorSchema
                    )
                  )
                )
              else -> schema
            }
          },
          Discriminator(discriminatorName.encodedName, discriminatorMappingOverride)
        )
    }
  }

  data class SRef(val info: SObjectInfo) : SchemaType() {
    override fun show(): String = "ref($info)"
  }


  data class SObjectInfo(val fullName: String, val typeParameterShortNames: List<String> = emptyList()) {
    companion object {
      val unit: SObjectInfo = SObjectInfo(fullName = "Unit")
    }
  }

  data class Discriminator(val propertyName: String, val mappingOverride: Map<String, SRef>)
}
