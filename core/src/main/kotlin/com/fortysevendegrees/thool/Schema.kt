package com.fortysevendegrees.thool

import arrow.core.Option
import arrow.core.tail
import java.io.InputStream
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.Duration as JavaDuration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID

/**
 * Describes the type `T`: its low-level representation, meta-data and validation rules.
 * @param format The name of the format of the low-level representation of `T`.
 */
data class Schema<T>(
  val schemaType: SchemaType,
  val isOptional: Boolean = false,
  val description: String? = null,
  // The default value together with the value encoded to a raw format, which will then be directly rendered as a
  // string in documentation. This is needed as codecs for nested types aren't available. Similar to Validator.EncodeToRaw
  val default: Pair<T, Any?>? = null,
  val format: String? = null,
  val encodedExample: Any? = null,
  val deprecated: Boolean = false,
  val validator: Validator<T> = Validator.pass()
) {

  fun <TT> map(f: (T) -> TT?, g: (TT) -> T): Schema<TT> =
    Schema(
      schemaType, isOptional, description,
      default?.let { (t, raw) ->
        f(t)?.let { tt -> Pair(tt, raw) }
      },
      format,
      encodedExample,
      deprecated,
      validator.contramap(g)
    )

  /** Returns an optional version of this schema, with `isOptional` set to true.
   */
  fun asOption(): Schema<Option<T>> =
    Schema(schemaType, true, description, null, format, encodedExample, deprecated, validator.asOptionElement())

  /** Returns an optional version of this schema, with `isOptional` set to true.
   */
  fun asNullable(): Schema<T?> =
    Schema(schemaType, true, description, null, format, encodedExample, deprecated, validator.asNullableElement())

  /** Returns an array version of this schema, with the schema type wrapped in [[SArray]].
   * Sets `isOptional` to true as the collection might be empty.
   */
  fun asArray(): Schema<Array<T>> =
    Schema(
      schemaType = SchemaType.SArray(this),
      isOptional = true,
      format = null,
      deprecated = deprecated,
      validator = validator.asArrayElements()
    )

  /** Returns a collection version of this schema, with the schema type wrapped in [[SArray]].
   * Sets `isOptional` to true as the collection might be empty.
   */
  fun asList(): Schema<List<T>> =
    Schema(
      schemaType = SchemaType.SArray(this),
      isOptional = true,
      format = null,
      deprecated = deprecated,
      validator = validator.asListElements()
    )

  fun description(d: String): Schema<T> = copy(description = d)

  fun encodedExample(e: Any): Schema<T> = copy(encodedExample = e)

  fun default(t: T, raw: Any? = null): Schema<T> =
    copy(default = Pair(t, raw), isOptional = true)

  fun format(f: String): Schema<T> =
    copy(format = f)

  fun deprecated(d: Boolean): Schema<T> =
    copy(deprecated = d)

  fun show(): String =
    "schema is $schemaType${if (isOptional) " (optional)" else ""}"

  fun <U> modifyUnsafe(vararg fields: String, modify: (Schema<U>) -> Schema<U>): Schema<T> =
    modifyAtPath(fields.toList(), modify)

  data class SCoproduct(
    override val info: SObjectInfo,
    val schemas: List<Schema<*>>,
    val discriminator: SchemaType.Discriminator?
  ) :
    SchemaType.SObject() {

    override fun show(): String = "oneOf:" + schemas.joinToString(",")

    fun <D> addDiscriminatorField(
      discriminatorName: FieldName,
      discriminatorSchema: Schema<D> = Schema.string(),
      discriminatorMappingOverride: Map<String, SRef> = emptyMap()
    ): SCoproduct =
      SCoproduct(
        info,
        schemas.map { s ->
          when (s.schemaType) {
            is SchemaType.SObject.SProduct ->
              s.copy(
                schemaType = s.schemaType.copy(
                  fields = s.schemaType.fields.toList() + Pair(
                    discriminatorName,
                    discriminatorSchema
                  )
                )
              )
            else -> s
          }
        },
        Discriminator(discriminatorName.encodedName, discriminatorMappingOverride)
      )
  }

  private fun <U> modifyAtPath(fieldPath: List<String>, modify: (Schema<U>) -> Schema<U>): Schema<T> =
    when {
      fieldPath.isEmpty() -> modify(this as Schema<U>) as Schema<T> // we don't have type-polymorphic functions (????)
      else -> {
        val head = fieldPath.first()
        val tail = fieldPath.tail()
        val newSchemaType = when {
          schemaType is SchemaType.SArray && head == ModifyCollectionElements -> SchemaType.SArray(
            schemaType.element.modifyAtPath(
              tail,
              modify
            )
          )
          schemaType is SchemaType.SObject.SProduct -> schemaType.copy(
            fields = schemaType.fields.map { field ->
              val (fieldName, fieldSchema) = field
              if (fieldName.name == head) Pair(fieldName, fieldSchema.modifyAtPath(tail, modify)) else field
            }
          )
          schemaType is SchemaType.SObject.SOpenProduct && head == ModifyCollectionElements ->
            schemaType.copy(valueSchema = schemaType.valueSchema.modifyAtPath(tail, modify))
          schemaType is SchemaType.SObject.SCoproduct ->
            schemaType.copy(schemas = schemaType.schemas.map { it.modifyAtPath(fieldPath, modify) })
          else -> schemaType
        }

        copy(schemaType = newSchemaType)
      }
    }

  fun validate(v: Validator<T>): Schema<T> =
    copy(validator = validator.and(v))

  companion object {
    val ModifyCollectionElements = "each"

    /** Creates a schema for type `T`, where the low-level representation is a `String`. */
    fun <T> string(): Schema<T> = Schema(SchemaType.SString)

    /** Creates a schema for type `T`, where the low-level representation is binary.*/
    fun <T> binary(): Schema<T> = Schema(SchemaType.SBinary)

    val string: Schema<String> = Schema(SchemaType.SString)

    val byte: Schema<Byte> = Schema(SchemaType.SInteger)

    val short: Schema<Short> = Schema(SchemaType.SInteger)

    val int: Schema<Int> = Schema(SchemaType.SInteger)

    val long: Schema<Long> = Schema<Long>(SchemaType.SInteger).format("int64")

    val float: Schema<Float> = Schema<Float>(SchemaType.SNumber).format("float")

    val double: Schema<Double> = Schema<Double>(SchemaType.SNumber).format("double")

    val boolean: Schema<Boolean> = Schema(SchemaType.SBoolean)

    val unit: Schema<Unit> = Schema(SchemaType.SObject.SProduct.Empty)

//    val schemaForFile: Schema<TapirFile> = Schema(SchemaType.SBinary)

    val byteArray: Schema<ByteArray> = Schema(SchemaType.SBinary)

    // JVM
    // Java NIO
    val byteBuffer: Schema<ByteBuffer> = Schema(SchemaType.SBinary)
    val inputStream: Schema<InputStream> = Schema(SchemaType.SBinary)

    // Java Date
    val instant: Schema<Instant> = Schema(SchemaType.SDateTime)
    val zonedDateTime: Schema<ZonedDateTime> = Schema(SchemaType.SDateTime)
    val offsetDateTime: Schema<OffsetDateTime> = Schema(SchemaType.SDateTime)
    val date: Schema<Date> = Schema(SchemaType.SDateTime)
    val localDateTime: Schema<LocalDateTime> = Schema(SchemaType.SString)
    val localDate: Schema<LocalDate> = Schema(SchemaType.SDate)
    val zoneOffset: Schema<ZoneOffset> = Schema(SchemaType.SString)
    val javaDuration: Schema<JavaDuration> = Schema(SchemaType.SString)
    val localTime: Schema<LocalTime> = Schema(SchemaType.SString)
    val offsetTime: Schema<OffsetTime> = Schema(SchemaType.SString)

    // Java Util
    val uuid: Schema<UUID> = Schema<UUID>(SchemaType.SString).format("uuid")

    // Java Math
    val bigDecimal: Schema<BigDecimal> = Schema(SchemaType.SString)
  }
}
