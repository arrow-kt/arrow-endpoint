package com.fortysevendeg.thool

import arrow.core.Option
import java.io.InputStream
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.reflect.KProperty1

public data class SchemaInfo<A>(
  val description: String? = null,
  /** The default value together with the value encoded to a raw format, which will then be directly rendered as a string in documentation */
  val default: Pair<A, Any?>? = null,
  val format: String? = null,
  val encodedExample: Any? = null,
  val deprecatedMessage: String? = null
) {
  val deprecated: Boolean = deprecatedMessage != null
}

public sealed interface Schema<A> {

  public val info: SchemaInfo<A>

  public fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B>

  public fun <B> map(f: (A) -> B?): Schema<B> =
    transformInfo {
      SchemaInfo(
        it.description,
        it.default?.let { (t, raw) ->
          f(t)?.let { tt -> Pair(tt, raw) }
        },
        it.format,
        it.encodedExample,
        it.deprecatedMessage
      )
    }

  /**
   * Returns an optional version of this schema, with `isOptional` set to true.
   */
  public fun asOption(): Schema<Option<A>> =
    Nullable(this, SchemaInfo(info.description, null, info.format, info.encodedExample, info.deprecatedMessage))

  /**
   * Returns an optional version of this schema, with `isOptional` set to true.
   */
  public fun asNullable(): Schema<A?> =
    Nullable(this, SchemaInfo(info.description, null, info.format, info.encodedExample, info.deprecatedMessage))

  /**
   * Returns an array version of this schema, with the schema type wrapped in [SchemaType.List].
   * Sets `isOptional` to true as the collection might be empty.
   */
  public fun asArray(): Schema<Array<A>> =
    List(
      this,
      SchemaInfo(
        format = null,
        deprecatedMessage = info.deprecatedMessage
      )
    )

  /** Returns a collection version of this schema, with the schema type wrapped in [SchemaType.List].
   * Sets `isOptional` to true as the collection might be empty.
   */
  public fun asList(): Schema<kotlin.collections.List<A>> =
    List(
      this,
      SchemaInfo(
        format = null,
        deprecatedMessage = info.deprecatedMessage
      )
    )

  public fun default(t: A, raw: Any? = null): Schema<A> =
    transformInfo { it.copy(default = Pair(t, raw)) }

  public fun description(d: kotlin.String): Schema<A> =
    transformInfo { it.copy(description = d) }

  public fun encodedExample(e: Any): Schema<A> =
    transformInfo { it.copy(encodedExample = e) }

  public fun format(f: kotlin.String): Schema<A> =
    transformInfo { it.copy(format = f) }

  public fun deprecatedMessage(d: kotlin.String): Schema<A> =
    transformInfo { it.copy(deprecatedMessage = d) }

  /**
   * Nullable & Collections are considered nullable. Collections because they can be empty.
   **/
  public fun isOptional(): kotlin.Boolean =
    this is Nullable || this is List

  public fun isNotOptional(): kotlin.Boolean = !isOptional()

  public data class String<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Schema<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = String(transform(info))
    override fun toString(): kotlin.String = "string"
  }

  public sealed interface NumberModifier
  public object Signed : NumberModifier
  public object Unsigned : NumberModifier

  public sealed interface NumberSize
  public object _8 : NumberSize
  public object _16 : NumberSize
  public object _32 : NumberSize
  public object _64 : NumberSize

  public sealed interface Number<A> : Schema<A> {
    public val modifier: NumberModifier
    public val size: NumberSize

    public data class Byte<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Number<A> {
      override val modifier: NumberModifier = Signed
      override val size: NumberSize = _8
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = Byte(transform(info))
      override fun toString(): kotlin.String = "byte"
    }

    public data class UByte<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Number<A> {
      override val modifier: NumberModifier = Unsigned
      override val size: NumberSize = _8
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = UByte(transform(info))
      override fun toString(): kotlin.String = "unsigned byte"
    }

    public data class Short<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Number<A> {
      override val modifier: NumberModifier = Signed
      override val size: NumberSize = _16
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = Short(transform(info))
      override fun toString(): kotlin.String = "short"
    }

    public data class UShort<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Number<A> {
      override val modifier: NumberModifier = Unsigned
      override val size: NumberSize = _16
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = UShort(transform(info))
      override fun toString(): kotlin.String = "unsigned short"
    }

    public data class Int<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Number<A> {
      override val modifier: NumberModifier = Signed
      override val size: NumberSize = _32
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = Int(transform(info))
      override fun toString(): kotlin.String = "int32"
    }

    public data class UInt<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Number<A> {
      override val modifier: NumberModifier = Unsigned
      override val size: NumberSize = _32
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = UInt(transform(info))
      override fun toString(): kotlin.String = "unsigned int32"
    }

    public data class Long<A>(override val info: SchemaInfo<A> = SchemaInfo(format = "int64")) : Number<A> {
      override val modifier: NumberModifier = Signed
      override val size: NumberSize = _64
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = Long(transform(info))
      override fun toString(): kotlin.String = "int64"
    }

    public data class ULong<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Number<A> {
      override val modifier: NumberModifier = Unsigned
      override val size: NumberSize = _64
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = ULong(transform(info))
      override fun toString(): kotlin.String = "unsigned int64"
    }

    public data class Float<A>(override val info: SchemaInfo<A> = SchemaInfo(format = "float")) : Number<A> {
      override val modifier: NumberModifier = Signed
      override val size: NumberSize = _32
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = Float(transform(info))
      override fun toString(): kotlin.String = "float"
    }

    public data class Double<A>(override val info: SchemaInfo<A> = SchemaInfo(format = "double")) : Number<A> {
      override val modifier: NumberModifier = Signed
      override val size: NumberSize = _64
      override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = Double(transform(info))
      override fun toString(): kotlin.String = "double"
    }
  }

  public data class Boolean<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Schema<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = Boolean(transform(info))
    override fun toString(): kotlin.String = "boolean"
  }

  public data class List<A>(
    val element: Schema<*>,
    override val info: SchemaInfo<A> = SchemaInfo()
  ) : Schema<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> =
      List(element, transform(info))

    override fun toString(): kotlin.String = "[$element]"
  }

  public data class Binary<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Schema<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = Binary(transform(info))
    override fun toString(): kotlin.String = "binary"
  }

  public data class Date<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Schema<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = Date(transform(info))
    override fun toString(): kotlin.String = "date"
  }

  public data class DateTime<A>(override val info: SchemaInfo<A> = SchemaInfo()) : Schema<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> = DateTime(transform(info))
    override fun toString(): kotlin.String = "date-time"
  }

  public data class Nullable<A>(
    val element: Schema<*>,
    override val info: SchemaInfo<A> = SchemaInfo()
  ) : Schema<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> =
      Nullable(element, transform(info))

    override fun toString(): kotlin.String = "$element?"
  }

  public sealed interface Object<A> : Schema<A> {
    public val objectInfo: ObjectInfo
  }

  public data class Either<A>(
    val left: Schema<*>,
    val right: Schema<*>,
    override val info: SchemaInfo<A> = SchemaInfo()
  ) : Object<A> {
    override val objectInfo: ObjectInfo =
      ObjectInfo("arrow.core.Either", listOf(left.toString(), right.toString()))

    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> =
      Either(left, right, transform(info))

    override fun toString(): kotlin.String = "either<$left, $right>"
  }

  /**
   * Represents an key-value set or Map<K, V>.
   * A Map contains N-fields of the same type [valueSchema] which are held by a corresponding key [keySchema].
   *
   * Map<Int, DateTime> =>
   *   Schema2.Map(
   *     Schema2.ObjectInfo("Map", listOf("Int", "DateTime")),
   *     Schema.int,
   *     Schema.dateTime
   *   )
   */
  public data class Map<A>(
    override val objectInfo: ObjectInfo,
    val keySchema: Schema<*>,
    val valueSchema: Schema<*>,
    override val info: SchemaInfo<A> = SchemaInfo()
  ) : Object<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> =
      Map(objectInfo, keySchema, valueSchema, transform(info))

    override fun toString(): kotlin.String = "$keySchema->$valueSchema"
  }

  /**
   * Represents an open-product or Map<String, V>.
   * An open product contains N-fields, which are held by [String] keys.
   *
   * Map<String, Int> =>
   *   Schema2.OpenProduct(
   *     Schema2.ObjectInfo("Map", listOf("String", "Int")),
   *     Schema.int
   *   )
   */
  public data class OpenProduct<A>(
    override val objectInfo: ObjectInfo,
    val valueSchema: Schema<*>,
    override val info: SchemaInfo<A> = SchemaInfo()
  ) : Object<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> =
      OpenProduct(objectInfo, valueSchema, transform(info))

    override fun toString(): kotlin.String = "String->$valueSchema"
  }

  /**
   * Represents a product type.
   * A product type has [ObjectInfo] & a fixed set of [fields]
   *
   * public data class Person(val name: String, val age: Int)
   *
   * Person =>
   *   Schema2.Product(
   *     ObjectInfo("Person"),
   *     listOf(
   *       Pair(FieldName("name"), Schema.string),
   *       Pair(FieldName("age"), Schema.int)
   *     )
   *   )
   */
  public data class Product<A>(
    override val objectInfo: ObjectInfo,
    val fields: kotlin.collections.List<Pair<FieldName, Schema<*>>>,
    override val info: SchemaInfo<A> = SchemaInfo()
  ) : Object<A> {
    public fun required(): kotlin.collections.List<FieldName> =
      fields.mapNotNull { (f, s) -> if (!s.isOptional()) f else null }

    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> =
      Product(objectInfo, fields, transform(info))

    override fun toString(): kotlin.String =
      "${objectInfo.fullName}(${fields.joinToString(",") { (f, s) -> "$f=$s" }})"

    public companion object {
      public val Empty = Product<Unit>(ObjectInfo.unit, emptyList())
    }
  }

  /**
   * Represents a value in an enum class
   * A product of [kotlin.Enum.name] and [kotlin.Enum.ordinal]
   */
  public data class EnumValue(val name: kotlin.String, val ordinal: Int)

  /**
   * Represents an Enum
   * Has [ObjectInfo], and list of its values.
   *
   * enum class Test { A, B, C; }
   *
   * Test =>
   *   Schema2.Enum(
   *     Schema2.ObjectInfo("Test"),
   *     listOf(
   *       Schema2.EnumValue("A", 0),
   *       Schema2.EnumValue("B", 1),
   *       Schema2.EnumValue("C", 2)
   *     )
   *   )
   */
  public data class Enum<A>(
    override val objectInfo: ObjectInfo,
    val values: kotlin.collections.List<EnumValue>,
    override val info: SchemaInfo<A> = SchemaInfo()
  ) : Object<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> =
      Enum(objectInfo, values, transform(info))

    override fun toString(): kotlin.String =
      "${objectInfo.fullName}[${values.joinToString(separator = " | ")}]"
  }

  /**
   * Represents a sum or coproduct type.
   * Has [ObjectInfo], and NonEmptyList of subtypes schemas.
   * These subtype schemas contain all details about the subtypes, since they'll all have Schema2 is Schema2.Object.
   *
   * Either<A, B> =>
   *   Schema2.Coproduct(
   *     Schema2.ObjectInfo("Either", listOf("A", "B")),
   *     listOf(
   *       Schema2.Product("Either.Left", listOf("value", schemeA)),
   *       Schema2.Product("Either.Right", listOf("value", schemeA)),
   *     )
   *   )
   */
  public data class Coproduct<A>(
    override val objectInfo: ObjectInfo,
    val schemas: arrow.core.NonEmptyList<Schema<*>>,
    override val info: SchemaInfo<A> = SchemaInfo()
  ) : Object<A> {
    override fun <B> transformInfo(transform: (SchemaInfo<A>) -> SchemaInfo<B>): Schema<B> =
      Coproduct(objectInfo, schemas, transform(info))

    override fun toString(): kotlin.String =
      "${objectInfo.fullName}[${schemas.joinToString(separator = " | ")}]"
  }

  /**
   * ObjectInfo contains the fullName of an object, and the type-param names.
   *
   * Either<A, B> => ObjectInfo("Either", listOf("A", "B"))
   */
  public data class ObjectInfo(
    val fullName: kotlin.String,
    val typeParameterShortNames: kotlin.collections.List<kotlin.String> = emptyList()
  ) {
    public companion object {
      public val unit: ObjectInfo = ObjectInfo(fullName = "Unit")
    }
  }

  public companion object {
    /** Creates a schema for type `T`, where the low-level representation is a `String`. */
    public fun <T> string(): Schema<T> = String()

    /** Creates a schema for type `T`, where the low-level representation is binary.*/
    public fun <T> binary(): Schema<T> = Binary()

    public val string: Schema<kotlin.String> = String()

    @ExperimentalUnsignedTypes
    public val ubyte: Schema<UByte> = Number.UByte()

    public val byte: Schema<Byte> = Schema.Number.Byte()

    @ExperimentalUnsignedTypes
    public val ushort: Schema<UShort> = Number.UShort()

    public val short: Schema<Short> = Schema.Number.Short()

    @ExperimentalUnsignedTypes
    public val uint: Schema<UInt> = Number.UInt()

    public val int: Schema<Int> = Schema.Number.Int()

    @ExperimentalUnsignedTypes
    public val ulong: Schema<ULong> = Number.ULong()

    public val long: Schema<Long> = Schema.Number.Long()

    public val float: Schema<Float> = Number.Float()

    public val double: Schema<Double> = Number.Double()

    public val boolean: Schema<kotlin.Boolean> = Boolean()

    public val unit: Schema<Unit> = Schema.Product.Empty

//    val schemaForFile: Schema<ThoolFile> = Schema(SchemaType.Binary)

    public val byteArray: Schema<ByteArray> = Schema.binary()

    public fun <A : kotlin.Enum<A>> enum(name: kotlin.String, enumValues: Array<out A>): Schema<A> =
      Enum(
        Schema.ObjectInfo(name),
        enumValues.map { EnumValue(it.name, it.ordinal) }
      )

    public inline fun <reified A : kotlin.Enum<A>> enum(): Schema<A> =
      enum(requireNotNull(A::class.qualifiedName) { "Qualified name on KClass should never be null." }, enumValues())

    // JVM
    // Java NIO
    public val byteBuffer: Schema<ByteBuffer> = Schema.binary()
    public val inputStream: Schema<InputStream> = Schema.binary()

    // Java Date
    public val instant: Schema<Instant> = Schema.DateTime()
    public val zonedDateTime: Schema<ZonedDateTime> = Schema.DateTime()
    public val offsetDateTime: Schema<OffsetDateTime> = Schema.DateTime()
    public val date: Schema<java.util.Date> = Schema.DateTime()

    public val localDateTime: Schema<LocalDateTime> = Schema.String()
    public val localDate: Schema<LocalDate> = Schema.String()
    public val zoneOffset: Schema<ZoneOffset> = Schema.String()
    public val javaDuration: Schema<Duration> = Schema.String()
    public val localTime: Schema<LocalTime> = Schema.String()
    public val offsetTime: Schema<OffsetTime> = Schema.String()

    // Java Util
    public val uuid: Schema<UUID> = Schema.string<UUID>().format("uuid")

    // Java Math
    public val bigDecimal: Schema<BigDecimal> = Schema.string()
  }
}

public inline fun <reified A> Schema<A>.asOpenProduct(): Schema<Map<String, A>> =
  Schema.OpenProduct(
    Schema.ObjectInfo(
      "Map",
      listOf(requireNotNull(A::class.qualifiedName) { "Qualified name on KClass should never be null." })
    ),
    this
  )

public inline fun <reified A> Schema.Companion.product(
  vararg properties: Pair<KProperty1<A, *>, Schema<*>>
): Schema<A> =
  Schema.Product(
    Schema.ObjectInfo(requireNotNull(A::class.qualifiedName) { "Qualified name on KClass should never be null." }),
    properties.map { (prop, schema) -> FieldName(prop.name) to schema }
  )
