package com.fortysevendegrees.thool

import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.Cookie
import com.fortysevendegrees.thool.model.HeaderNames
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.QueryParams
import kotlinx.coroutines.flow.Flow
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object Thool {

  operator fun <A> invoke(f: Thool.() -> A): A = f(Thool)

  @JvmName("queryList")
  fun <A> query(name: String, codec: Codec<List<String>, A, CodecFormat.TextPlain>): EndpointInput.Query<A> =
    EndpointInput.Query(name, codec, EndpointIO.Info.empty())

  fun <A> query(name: String, codec: PlainCodec<A>): EndpointInput.Query<A> =
    EndpointInput.Query(name, Codec.listHead(codec), EndpointIO.Info.empty())

  fun queryParams(): EndpointInput.QueryParams<QueryParams> =
    EndpointInput.QueryParams(Codec.idPlain(), EndpointIO.Info.empty())

  fun <A> path(name: String, codec: PlainCodec<A>): EndpointInput.PathCapture<A> =
    EndpointInput.PathCapture(name, codec, EndpointIO.Info.empty())

  fun <A> path(codec: PlainCodec<A>): EndpointInput.PathCapture<A> =
    EndpointInput.PathCapture(null, codec, EndpointIO.Info.empty())

  fun paths(): EndpointInput.PathsCapture<List<String>> =
    EndpointInput.PathsCapture(Codec.idPlain(), EndpointIO.Info.empty())

  fun <A> header(name: String, codec: Codec<List<String>, A, CodecFormat.TextPlain>): EndpointIO.Header<A> =
    EndpointIO.Header(name, codec, EndpointIO.Info.empty())

//  fun header(h: Header): EndpointIO.FixedHeader<Unit> = EndpointIO.FixedHeader(h, Codec.idPlain(), EndpointIO.Info.empty())
//  fun header(name: String, value: String): EndpointIO.FixedHeader<Unit> = header(Header(name, value))
//  fun headers(): EndpointIO.Headers<List<Header>> = EndpointIO.Headers(Codec.idPlain(), EndpointIO.Info.empty())

  // TODO: cache directives
  fun <A> cookie(name: String, codec: Codec<String?, A, CodecFormat.TextPlain>): EndpointInput.Cookie<A> =
    EndpointInput.Cookie(name, codec, EndpointIO.Info.empty())

  fun cookies(): EndpointIO.Header<List<Cookie>> =
    header(HeaderNames.Cookie, Codec.cookiesCodec)

  fun fixedPath(s: String): EndpointInput.FixedPath<Unit> =
    EndpointInput.FixedPath(s, Codec.idPlain(), EndpointIO.Info.empty())

  fun stringBody(charset: String): EndpointIO.Body<String, String> =
    stringBody(Charset.forName(charset))

  fun stringBody(charset: Charset = StandardCharsets.UTF_8): EndpointIO.Body<String, String> =
    EndpointIO.Body(RawBodyType.StringBody(charset), Codec.string, EndpointIO.Info.empty())

  val htmlBodyUtf8: EndpointIO.Body<String, String> =
    EndpointIO.Body(
      RawBodyType.StringBody(StandardCharsets.UTF_8),
      Codec.string.format(CodecFormat.TextHtml),
      EndpointIO.Info.empty()
    )

  fun <A> plainBody(
    codec: PlainCodec<A>,
    charset: Charset = StandardCharsets.UTF_8
  ): EndpointIO.Body<String, A> =
    EndpointIO.Body(RawBodyType.StringBody(charset), codec, EndpointIO.Info.empty())

  /** A body in any format, read using the given `codec`, from a raw string read using `charset`.*/
  fun <A, CF : CodecFormat> anyFromStringBody(
    codec: Codec<String, A, CF>,
    charset: Charset = StandardCharsets.UTF_8
  ): EndpointIO.Body<String, A> =
    EndpointIO.Body(RawBodyType.StringBody(charset), codec, EndpointIO.Info.empty())

  /**
   * Json codecs are usually derived from json-library-specific implicits. That's why integrations with
   * various json libraries define `jsonBody` methods, which directly require the library-specific implicits.
   *
   * If you have a custom json codec, you should use this method instead.
   */
  fun <A> anyJsonBody(codec: JsonCodec<A>): EndpointIO.Body<String, A> =
    anyFromStringBody(codec)

  /** Implement your own xml codec using `Codec.xml()` before using this method.
   */
  fun <A> xmlBody(codec: XmlCodec<A>): EndpointIO.Body<String, A> =
    anyFromStringBody(codec)

  /**
   * @param schema Schema of the body. This should be a schema for the "deserialized" stream.
   * @param charset An optional charset of the resulting stream's data, to be used in the content type.
   */
  fun flowBody(schema: Schema<Flow<Byte>>, format: CodecFormat, charset: Charset? = null) =
    EndpointIO.StreamBody(Codec.id(format, schema), EndpointIO.Info.empty(), charset)

  fun method(m: Method): EndpointInput.FixedMethod<Unit> =
    EndpointInput.FixedMethod(m, Codec.idPlain(), EndpointIO.Info.empty())
}
