package arrow.endpoint

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.InputStream
import java.math.BigDecimal
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

class SchemaKtTest : StringSpec({

  "primitives" {
    Schema.reflect<Byte>() shouldBe Schema.byte
    Schema.reflect<UByte>() shouldBe Schema.ubyte
    Schema.reflect<Short>() shouldBe Schema.short
    Schema.reflect<UShort>() shouldBe Schema.ushort
    Schema.reflect<Int>() shouldBe Schema.int
    Schema.reflect<UInt>() shouldBe Schema.uint
    Schema.reflect<Long>() shouldBe Schema.long
    Schema.reflect<ULong>() shouldBe Schema.ulong
    Schema.reflect<Float>() shouldBe Schema.float
    Schema.reflect<Double>() shouldBe Schema.double
    Schema.reflect<String>() shouldBe Schema.string
    Schema.reflect<Boolean>() shouldBe Schema.boolean
    Schema.reflect<Unit>() shouldBe Schema.unit
    Schema.reflect<ByteArray>() shouldBe Schema.byteArray
    Schema.reflect<InputStream>() shouldBe Schema.inputStream
    Schema.reflect<Instant>() shouldBe Schema.instant
    Schema.reflect<ZonedDateTime>() shouldBe Schema.zonedDateTime
    Schema.reflect<OffsetDateTime>() shouldBe Schema.offsetDateTime
    Schema.reflect<Date>() shouldBe Schema.date
    Schema.reflect<LocalDateTime>() shouldBe Schema.localDateTime
    Schema.reflect<LocalDate>() shouldBe Schema.localDate
    Schema.reflect<ZoneOffset>() shouldBe Schema.zoneOffset
    Schema.reflect<Duration>() shouldBe Schema.javaDuration
    Schema.reflect<LocalTime>() shouldBe Schema.localTime
    Schema.reflect<OffsetTime>() shouldBe Schema.offsetTime
    Schema.reflect<BigDecimal>() shouldBe Schema.bigDecimal
    Schema.reflect<UUID>() shouldBe Schema.string()
  }

  "enum derivation" {
    Schema.reflect<Test>() shouldBe Schema.enum()
  }

  "public data class derivation" {
    Schema.reflect<Person>() shouldBe Schema.person()
  }

  "sealed class derivation" {
    Schema.reflect<Sum>() shouldBe Schema.sum()
  }
})

enum class Test { A, B, C; }

public data class Person(val name: String, val age: Int)

fun Schema.Companion.person(): Schema<Person> =
  Schema.Product(
    Schema.ObjectInfo(Person::class.qualifiedName!!),
    listOf(
      Pair(FieldName("name"), Schema.string),
      Pair(FieldName("age"), Schema.int)
    )
  )

sealed class Sum
public data class SumA(val test: Test, val person: Person) : Sum()
public data class SumB(val age: Int, val name: String, val person: Person) : Sum()

fun Schema.Companion.sumA(): Schema<SumA> =
  Schema.Product(
    Schema.ObjectInfo(SumA::class.qualifiedName!!),
    listOf(
      Pair(FieldName("test"), enum<Test>()),
      Pair(FieldName("person"), person())
    )
  )

fun Schema.Companion.sumB(): Schema<SumB> =
  Schema.Product(
    Schema.ObjectInfo(SumB::class.qualifiedName!!),
    listOf(
      Pair(FieldName("age"), int),
      Pair(FieldName("name"), string),
      Pair(FieldName("person"), person())
    )
  )

fun Schema.Companion.sum(): Schema<Sum> =
  Schema.Coproduct(
    Schema.ObjectInfo(Sum::class.qualifiedName!!),
    nonEmptyListOf(sumA(), sumB())
  )
