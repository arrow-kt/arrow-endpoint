package arrow.endpoint

import arrow.endpoint.model.CodecFormat
import io.ktor.utils.io.charsets.Charset
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
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import java.util.Date

public val Codec.Companion.uuid: Codec<String, UUID, CodecFormat.TextPlain>
  get() = stringCodec(Schema.uuid, UUID::fromString)

public val Codec.Companion.bigDecimal: Codec<String, BigDecimal, CodecFormat.TextPlain>
  get() = stringCodec(Schema.bigDecimal, ::BigDecimal)

public val Codec.Companion.localTime: Codec<String, LocalTime, CodecFormat.TextPlain>
  get() = string.map({ LocalTime.parse(it) }, DateTimeFormatter.ISO_LOCAL_TIME::format).schema(Schema.localTime)

public val Codec.Companion.localDate: Codec<String, LocalDate, CodecFormat.TextPlain>
  get() = string.map({ LocalDate.parse(it) }, DateTimeFormatter.ISO_LOCAL_DATE::format).schema(Schema.localDate)

public val Codec.Companion.offsetDateTime: Codec<String, OffsetDateTime, CodecFormat.TextPlain>
  get() = string.map({ OffsetDateTime.parse(it) }, DateTimeFormatter.ISO_OFFSET_DATE_TIME::format)
    .schema(Schema.offsetDateTime)

public val Codec.Companion.zonedDateTime: Codec<String, ZonedDateTime, CodecFormat.TextPlain>
  get() = string.map({ ZonedDateTime.parse(it) }, DateTimeFormatter.ISO_ZONED_DATE_TIME::format)
    .schema(Schema.zonedDateTime)

public val Codec.Companion.instant: Codec<String, Instant, CodecFormat.TextPlain>
  get() = string.map({ Instant.parse(it) }, DateTimeFormatter.ISO_INSTANT::format).schema(Schema.instant)

public val Codec.Companion.date: Codec<String, Date, CodecFormat.TextPlain>
  get() = instant.map({ Date.from(it) }, { it.toInstant() }).schema(Schema.date)

public val Codec.Companion.zoneOffset: Codec<String, ZoneOffset, CodecFormat.TextPlain>
  get() = stringCodec(Schema.zoneOffset, ZoneOffset::of)

public val Codec.Companion.javaDuration: Codec<String, JavaDuration, CodecFormat.TextPlain>
  get() = stringCodec(Schema.javaDuration, JavaDuration::parse)

public val Codec.Companion.offsetTime: Codec<String, OffsetTime, CodecFormat.TextPlain>
  get() = string.map({ OffsetTime.parse(it) }, DateTimeFormatter.ISO_OFFSET_TIME::format).schema(Schema.offsetTime)

public val Codec.Companion.localDateTime: Codec<String, LocalDateTime, CodecFormat.TextPlain>
  get() = string.mapDecode({ l ->
    try {
      try {
        DecodeResult.Value(LocalDateTime.parse(l))
      } catch (e: DateTimeParseException) {
        DecodeResult.Value(OffsetDateTime.parse(l).toLocalDateTime())
      }
    } catch (e: Exception) {
      DecodeResult.Failure.Error(l, e)
    }
  }) { h -> OffsetDateTime.of(h, ZoneOffset.UTC).toString() }
    .schema(Schema.localDateTime)

public val Codec.Companion.inputStream: Codec<InputStream, InputStream, CodecFormat.OctetStream>
  get() = id(CodecFormat.OctetStream, Schema.inputStream)

public val Codec.Companion.byteBuffer: Codec<ByteBuffer, ByteBuffer, CodecFormat.OctetStream>
  get() = id(CodecFormat.OctetStream, Schema.byteBuffer)

public val Codec.Companion.formSeqCodecUtf8: Codec<String, List<Pair<String, String>>, CodecFormat.XWwwFormUrlencoded>
  get() = formSeqCodec(Charsets.UTF_8)

public val Codec.Companion.formMapCodecUtf8: Codec<String, Map<String, String>, CodecFormat.XWwwFormUrlencoded>
  get() = formMapCodec(Charsets.UTF_8)

public fun formMapCodec(charset: Charset): Codec<String, Map<String, String>, CodecFormat.XWwwFormUrlencoded> =
  formSeqCodec(charset).map({ it.toMap() }) { it.toList() }

public fun formSeqCodec(charset: Charset): Codec<String, List<Pair<String, String>>, CodecFormat.XWwwFormUrlencoded> =
  Codec.string.format(CodecFormat.XWwwFormUrlencoded).map({ UrlencodedData.decode(it, charset) }) {
    UrlencodedData.encode(
      it,
      charset
    )
  }
