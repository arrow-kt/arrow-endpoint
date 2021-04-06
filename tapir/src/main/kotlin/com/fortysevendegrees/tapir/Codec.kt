package com.fortysevendegrees.tapir

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.andThen
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.Cookie
import java.io.InputStream
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
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

typealias PlainCodec<A> = Codec<String, A, CodecFormat.TextPlain>
typealias JsonCodec<A> = Codec<String, A, CodecFormat.Json>
typealias XmlCodec<A> = Codec<String, A, CodecFormat.Xml>

interface Codec<L, H, out CF : CodecFormat> : Mapping<L, H> {
  fun schema(): Schema<H>
  val format: CF

  override fun validator(): Validator<H> =
    schema().validator

  override fun <HH> map(codec: Mapping<H, HH>): Codec<L, HH, CF> =
    object : Codec<L, HH, CF> {
      override fun rawDecode(l: L): DecodeResult<HH> =
        this@Codec.rawDecode(l).flatMap(codec::rawDecode)

      override fun encode(hh: HH): L =
        this@Codec.encode(codec.encode(hh))

      override val format: CF = this@Codec.format

      override fun schema(): Schema<HH> =
        this@Codec.schema()
          .map({ v ->
            when (val res = codec.decode(v)) {
              is DecodeResult.Failure -> null
              is DecodeResult.Value -> res.value
            }
          }, codec::encode)
          .validate(codec.validator())
    }

  fun <HH> mapDecode(f: (H) -> DecodeResult<HH>, g: (HH) -> H): Codec<L, HH, CF> =
    map(Mapping.fromDecode(f, g))

  fun <HH> map(f: (H) -> HH, g: (HH) -> H): Codec<L, HH, CF> =
    mapDecode(f.andThen { DecodeResult.Value(it) }, g)

  fun schema(s2: Schema<H>?): Codec<L, H, CF> =
    s2?.let { s2 ->
      object : Codec<L, H, CF> {
        override fun rawDecode(l: L): DecodeResult<H> = this@Codec.decode(l)
        override fun encode(h: H): L = this@Codec.encode(h)
        override fun schema(): Schema<H> = s2
        override val format: CF = this@Codec.format
      }
    } ?: this@Codec

  fun modifySchema(modify: (Schema<H>) -> Schema<H>): Codec<L, H, CF> =
    schema(modify(schema()))

  fun <CF2 : CodecFormat> format(f: CF2): Codec<L, H, CF2> =
    object : Codec<L, H, CF2> {
      override fun rawDecode(l: L): DecodeResult<H> = this@Codec.decode(l)
      override fun encode(h: H): L = this@Codec.encode(h)
      override fun schema(): Schema<H> = this@Codec.schema()
      override val format: CF2 = f
    }

  override fun validate(v: Validator<H>): Codec<L, H, CF> =
    schema(schema().validate(addEncodeToEnumValidator(v)))

  override fun decode(l: L): DecodeResult<H> {
    val res = super.decode(l)
    val default = schema().default
    return when {
      res is DecodeResult.Failure.Missing && default != null ->
        DecodeResult.Value(default.first)
      else -> res
    }
  }

  companion object {
    fun <L, CF : CodecFormat> id(f: CF, s: Schema<L>): Codec<L, L, CF> =
      object : Codec<L, L, CF> {
        override fun rawDecode(l: L): DecodeResult<L> = DecodeResult.Value(l)
        override fun encode(h: L): L = h
        override fun schema(): Schema<L> = s
        override val format: CF = f
      }

    fun <L> idPlain(s: Schema<L> = Schema(SchemaType.SString)): Codec<L, L, CodecFormat.TextPlain> =
      id(CodecFormat.TextPlain, s)

    fun <T> stringCodec(schema: Schema<T>, parse: (String) -> T): Codec<String, T, CodecFormat.TextPlain> =
      string.map(parse) { it.toString() }.schema(schema)

    val string: Codec<String, String, CodecFormat.TextPlain> =
      id(CodecFormat.TextPlain, Schema.string)

    val byte: Codec<String, Byte, CodecFormat.TextPlain> = stringCodec(Schema.byte) { it.toByte() }
    val short: Codec<String, Short, CodecFormat.TextPlain> = stringCodec(Schema.short) { it.toShort() }
    val int: Codec<String, Int, CodecFormat.TextPlain> = stringCodec(Schema.int) { it.toInt() }
    val long: Codec<String, Long, CodecFormat.TextPlain> = stringCodec(Schema.long) { it.toLong() }
    val float: Codec<String, Float, CodecFormat.TextPlain> = stringCodec(Schema.float) { it.toFloat() }
    val double: Codec<String, Double, CodecFormat.TextPlain> = stringCodec(Schema.double) { it.toDouble() }
    val boolean: Codec<String, Boolean, CodecFormat.TextPlain> = stringCodec(Schema.boolean) { it.toBoolean() }

    val uuid: Codec<String, UUID, CodecFormat.TextPlain> = stringCodec(Schema.uuid, UUID::fromString)
    val bigDecimal: Codec<String, BigDecimal, CodecFormat.TextPlain> = stringCodec(Schema.bigDecimal, ::BigDecimal)
    val localTime: Codec<String, LocalTime, CodecFormat.TextPlain> =
      string.map({ LocalTime.parse(it) }, DateTimeFormatter.ISO_LOCAL_TIME::format).schema(Schema.localTime)

    val localDate: Codec<String, LocalDate, CodecFormat.TextPlain> =
      string.map({ LocalDate.parse(it) }, DateTimeFormatter.ISO_LOCAL_DATE::format).schema(Schema.localDate)

    val offsetDateTime: Codec<String, OffsetDateTime, CodecFormat.TextPlain> =
      string.map({ OffsetDateTime.parse(it) }, DateTimeFormatter.ISO_OFFSET_DATE_TIME::format)
        .schema(Schema.offsetDateTime)

    val zonedDateTime: Codec<String, ZonedDateTime, CodecFormat.TextPlain> =
      //string.com.fortysevendegrees.tapir.map(ZonedDateTime.parse(_))(DateTimeFormatter.ISO_ZONED_DATE_TIME.format).schema(com.fortysevendegrees.tapir.Schema.schemaForZonedDateTime)
      string.map({ ZonedDateTime.parse(it) }, DateTimeFormatter.ISO_ZONED_DATE_TIME::format)
        .schema(Schema.zonedDateTime)

    val instant: Codec<String, Instant, CodecFormat.TextPlain> =
      string.map({ Instant.parse(it) }, DateTimeFormatter.ISO_INSTANT::format).schema(Schema.instant)

    val date: Codec<String, Date, CodecFormat.TextPlain> =
      instant.map({ Date.from(it) }, { it.toInstant() }).schema(Schema.date)

    val zoneOffset: Codec<String, ZoneOffset, CodecFormat.TextPlain> =
      stringCodec(Schema.zoneOffset, ZoneOffset::of)

    val javaDuration: Codec<String, JavaDuration, CodecFormat.TextPlain> =
      stringCodec(Schema.javaDuration, JavaDuration::parse)

    val offsetTime: Codec<String, OffsetTime, CodecFormat.TextPlain> =
      string.map({ OffsetTime.parse(it) }, DateTimeFormatter.ISO_OFFSET_TIME::format).schema(Schema.offsetTime)

    val localDateTime: Codec<String, LocalDateTime, CodecFormat.TextPlain> =
      string.mapDecode({ l ->
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

    // Uri type from com.fortysevendegrees.tapir.Tapir
//    val uri: com.fortysevendegrees.tapir.PlainCodec<Uri> =
//    string.mapDecode(raw => Uri.parse(raw).fold(e => com.fortysevendegrees.tapir.DecodeResult.Error(raw, new IllegalArgumentException(e)), com.fortysevendegrees.tapir.DecodeResult.Value(_)))(
//    _.toString()
//    )

    val byteArray: Codec<ByteArray, ByteArray, CodecFormat.OctetStream> = id(CodecFormat.OctetStream, Schema.byteArray)
    val inputStream: Codec<InputStream, InputStream, CodecFormat.OctetStream> =
      id(CodecFormat.OctetStream, Schema.inputStream)
    val byteBuffer: Codec<ByteBuffer, ByteBuffer, CodecFormat.OctetStream> =
      id(CodecFormat.OctetStream, Schema.byteBuffer)

    val formSeqCodecUtf8: Codec<String, List<Pair<String, String>>, CodecFormat.XWwwFormUrlencoded> =
      formSeqCodec(StandardCharsets.UTF_8)

    val formMapCodecUtf8: Codec<String, Map<String, String>, CodecFormat.XWwwFormUrlencoded> =
      formMapCodec(StandardCharsets.UTF_8)

    fun formSeqCodec(charset: Charset): Codec<String, List<Pair<String, String>>, CodecFormat.XWwwFormUrlencoded> =
      string.format(CodecFormat.XWwwFormUrlencoded).map({ UrlencodedData.decode(it, charset) }) {
        UrlencodedData.encode(
          it,
          charset
        )
      }

    fun formMapCodec(charset: Charset): Codec<String, Map<String, String>, CodecFormat.XWwwFormUrlencoded> =
      formSeqCodec(charset).map({ it.toMap() }) { it.toList() }

    private fun <A, B, CF : CodecFormat> listBinarySchema(c: Codec<A, B, CF>): Codec<List<A>, List<B>, CF> =
      id(c.format, Schema.binary<List<A>>())
        .mapDecode({ aas -> aas.map(c::decode).sequence() }) { bbs -> bbs.map(c::encode) }

    /**
     * Create a codec which requires that a list of low-level values contains a single element. Otherwise a decode
     * failure is returned. The given base codec `c` is used for decoding/encoding.
     *
     * The schema and validator are copied from the base codec.
     */
    fun <A, B, CF : CodecFormat> listHead(c: Codec<A, B, CF>): Codec<List<A>, B, CF> =
      listBinarySchema(c)
        .mapDecode({ list ->
          when (list.size) {
            0 -> DecodeResult.Failure.Missing
            1 -> DecodeResult.Value(list[0])
            else -> DecodeResult.Failure.Multiple(list)
          }
        }) { listOf(it) }
        .schema(c.schema())

    /**
     * Create a codec which decodes/encodes a list of low-level values to a list of high-level values, using the given base codec `c`.
     *
     * The schema is copied from the base codec.
     */
    fun <A, B, CF : CodecFormat> list(c: Codec<A, B, CF>): Codec<List<A>, List<B>, CF> =
      listBinarySchema(c).schema(c.schema().asList())

    /**
     * Create a codec which decodes/encodes an optional low-level value to an optional high-level value.
     * The given base codec `c` is used for decoding/encoding.
     *
     * The schema and validator are copied from the base codec.
     */
    fun <A, B, CF : CodecFormat> option(c: Codec<A, B, CF>): Codec<Option<A>, Option<B>, CF> =
      id(c.format, Schema.binary<Option<A>>())
        .mapDecode({ option ->
          when (option) {
            None -> DecodeResult.Value(None)
            is Some -> c.decode(option.value).map(::Some)
          }
        }) { us -> us.map(c::encode) }
        .schema(c.schema().asOption())

    /**
     * Create a codec which decodes/encodes an nullable low-level value to an optional high-level value.
     * The given base codec `c` is used for decoding/encoding.
     *
     * The schema and validator are copied from the base codec.
     */
    fun <A : Any, B : Any, CF : CodecFormat> nullable(c: Codec<A, B, CF>): Codec<A?, B?, CF> =
      id(c.format, Schema.binary<A?>())
        .mapDecode({ option ->
          when (option) {
            null -> DecodeResult.Value(null)
            else -> c.decode(option)
          }
        }) { us -> us?.let(c::encode) }
        .schema(c.schema().asNullable())

    fun <A> json(schema: Schema<A>, _rawDecode: (String) -> DecodeResult<A>, _encode: (A) -> String): JsonCodec<A> =
      anyStringCodec(schema, CodecFormat.Json, _rawDecode, _encode)

    fun <A> xml(schema: Schema<A>, rawDecode: (String) -> DecodeResult<A>, encode: (A) -> String): XmlCodec<A> =
      anyStringCodec(schema, CodecFormat.Xml, rawDecode, encode)

    private fun decodeCookie(cookie: String): DecodeResult<List<Cookie>> =
      when (val res = Cookie.parse(cookie)) {
        is Either.Left -> DecodeResult.Failure.Error(cookie, RuntimeException(res.value))
        is Either.Right -> DecodeResult.Value(res.value)
      }

    val cookieCodec: Codec<String, List<Cookie>, CodecFormat.TextPlain> =
      Codec.string.mapDecode(::decodeCookie) { cs -> cs.joinToString("; ") }

    val cookiesCodec: Codec<List<String>, List<Cookie>, CodecFormat.TextPlain> =
      Codec.list(cookieCodec).map(List<List<Cookie>>::flatten, ::listOf)

    fun <L, H, CF : CodecFormat> fromDecodeAndMeta(
      schema: Schema<H>,
      cf: CF,
      f: (L) -> DecodeResult<H>,
      g: (H) -> L
    ): Codec<L, H, CF> =
      object : Codec<L, H, CF> {
        override fun rawDecode(l: L): DecodeResult<H> = f(l)
        override fun encode(h: H): L = g(h)
        override fun schema(): Schema<H> = schema
        override val format: CF = cf
      }

    fun <A, CF : CodecFormat> anyStringCodec(
      schema: Schema<A>,
      cf: CF,
      rawDecode: (String) -> DecodeResult<A>,
      encode: (A) -> String
    ): Codec<String, A, CF> =
      fromDecodeAndMeta(schema, cf, { s: String ->
        val toDecode = if (schema.isOptional && s == "") "null" else s
        rawDecode(toDecode)
      }) { t -> if (schema.isOptional) "" else encode(t) }

  }
}


/** The raw format of the body: what do we need to know, to read it and pass to a codec for further decoding.
 */
sealed interface RawBodyType<R> {

  data class StringBody(val charset: Charset) : RawBodyType<String>

  sealed interface Binary<R> : RawBodyType<R>

  object ByteArrayBody : Binary<ByteArray>
  object ByteBufferBody : Binary<ByteBuffer>
  object InputStreamBody : Binary<InputStream>
//  object FileBody : Binary<TapirFile>

//  data class MultipartBody(val partTypes: Map<String, com.fortysevendegrees.tapir.RawBodyType<Any?>>, val defaultType: com.fortysevendegrees.tapir.RawBodyType<Any?>?) : com.fortysevendegrees.tapir.RawBodyType<List<RawPart>> {
//    fun partType(name: String): com.fortysevendegrees.tapir.RawBodyType<Any?>? = partTypes[name] ?: defaultType
//  }

  companion object
}