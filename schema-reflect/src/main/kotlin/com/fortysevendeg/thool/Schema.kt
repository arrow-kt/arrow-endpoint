package com.fortysevendeg.thool

import arrow.core.Nel
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
import java.util.Date
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility

public inline fun <reified A : Any> Schema.Companion.reflect(): Schema<A> =
  A::class.schema()

@OptIn(ExperimentalUnsignedTypes::class)
@Suppress("NO_REFLECTION_IN_CLASS_PATH", "UNCHECKED_CAST")
public fun <A : Any> KClass<A>.schema(): Schema<A> =
  when {
    this == String::class -> Schema.string as Schema<A>
    this == UByte::class -> Schema.ubyte as Schema<A>
    this == Byte::class -> Schema.byte as Schema<A>
    this == UShort::class -> Schema.ushort as Schema<A>
    this == Short::class -> Schema.short as Schema<A>
    this == UInt::class -> Schema.uint as Schema<A>
    this == Int::class -> Schema.int as Schema<A>
    this == ULong::class -> Schema.ulong as Schema<A>
    this == Long::class -> Schema.long as Schema<A>
    this == Float::class -> Schema.float as Schema<A>
    this == Double::class -> Schema.double as Schema<A>
    this == Boolean::class -> Schema.boolean as Schema<A>
    this == Unit::class -> Schema.unit as Schema<A>
    this == ByteArray::class -> Schema.byteArray as Schema<A>
    this == ByteBuffer::class -> Schema.byteBuffer as Schema<A>
    this == InputStream::class -> Schema.inputStream as Schema<A>
    this == BigDecimal::class -> Schema.bigDecimal as Schema<A>
    this == Instant::class -> Schema.instant as Schema<A>
    this == ZonedDateTime::class -> Schema.zonedDateTime as Schema<A>
    this == OffsetDateTime::class -> Schema.offsetDateTime as Schema<A>
    this == Date::class -> Schema.date as Schema<A>
    this == LocalDateTime::class -> Schema.localDateTime as Schema<A>
    this == LocalDate::class -> Schema.localDate as Schema<A>
    this == ZoneOffset::class -> Schema.zoneOffset as Schema<A>
    this == Duration::class -> Schema.javaDuration as Schema<A>
    this == LocalTime::class -> Schema.localTime as Schema<A>
    this == OffsetTime::class -> Schema.offsetTime as Schema<A>
    this == BigDecimal::class -> Schema.bigDecimal as Schema<A>
    this == UUID::class -> Schema.string()
    isSealed -> Schema.Coproduct(objectInfo(), sealedSubclassSchemas())
    isData -> Schema.Product(objectInfo(), properties())
    else -> (this as? KClass<Enum<*>>)?.let {
      Schema.Enum(
        objectInfo(),
        // MPP reflection??
        it.java.enumConstants?.toList()?.map { Schema.EnumValue(it.name, it.ordinal) } ?: emptyList()
      )
    } ?: TODO("No schema supported for $this")
  }

@Suppress("NO_REFLECTION_IN_CLASS_PATH")
public fun KClass<*>.sealedSubclassSchemas(): Nel<Schema<*>> =
  Nel.fromListUnsafe(sealedSubclasses.map { it.schema() })

public fun KClass<*>.objectInfo(): Schema.ObjectInfo =
  Schema.ObjectInfo(qualifiedName ?: "<anonymous>")

@Suppress("NO_REFLECTION_IN_CLASS_PATH")
public fun KClass<*>.properties(): List<Pair<FieldName, Schema<*>>> =
  constructors.firstOrNull { it.visibility == KVisibility.PUBLIC }
    ?.parameters.orEmpty().mapNotNull {
      val pName = it.name
      val cl = it.type.classifier as? KClass<*>

      if (pName != null && cl != null) FieldName(pName) to cl.schema()
      else null
    }
