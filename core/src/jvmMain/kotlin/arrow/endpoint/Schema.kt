@file:JvmName("SchemaUtils")

package arrow.endpoint

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

public val Schema.Companion.inputStream: Schema<InputStream>
  get() = binary()

public val Schema.Companion.instant: Schema<Instant>
  get() = Schema.DateTime()
public val Schema.Companion.zonedDateTime: Schema<ZonedDateTime>
  get() = Schema.DateTime()
public val Schema.Companion.offsetDateTime: Schema<OffsetDateTime>
  get() = Schema.DateTime()
public val Schema.Companion.date: Schema<java.util.Date>
  get() = Schema.DateTime()

public val Schema.Companion.localDateTime: Schema<LocalDateTime>
  get() = Schema.String()
public val Schema.Companion.localDate: Schema<LocalDate>
  get() = Schema.String()
public val Schema.Companion.zoneOffset: Schema<ZoneOffset>
  get() = Schema.String()
public val Schema.Companion.javaDuration: Schema<Duration>
  get() = Schema.String()
public val Schema.Companion.localTime: Schema<LocalTime>
  get() = Schema.String()
public val Schema.Companion.offsetTime: Schema<OffsetTime>
  get() = Schema.String()

public val Schema.Companion.uuid: Schema<UUID>
  get() = string<UUID>().format("uuid")

public val Schema.Companion.bigDecimal: Schema<BigDecimal>
  get() = string()

public val Schema.Companion.byteBuffer: Schema<ByteBuffer>
  get() = binary()
