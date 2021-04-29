package com.fortysevendegrees.thool

import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.Cookie
import com.fortysevendegrees.thool.model.Headers
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.QueryParams
import com.fortysevendegrees.thool.model.StatusCode
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object Thool {

  inline operator fun <A> invoke(f: Thool.() -> A): A = f(Thool)

  @JvmName("queryList")
  fun <A> query(name: String, codec: Codec<List<String>, A, CodecFormat.TextPlain>): EndpointInput.Query<A> =
    EndpointInput.Query(name, codec, EndpointIO.Info.empty())

  fun <A> query(name: String, codec: PlainCodec<A>): EndpointInput.Query<A> =
    EndpointInput.Query(name, Codec.listFirst(codec), EndpointIO.Info.empty())

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
    header(Headers.Cookie, Codec.cookiesCodec)

  fun fixedPath(s: String): EndpointInput.FixedPath<Unit> =
    EndpointInput.FixedPath(s, Codec.idPlain(), EndpointIO.Info.empty())

  fun stringBody(charset: String): EndpointIO.StringBody<String> =
    stringBody(Charset.forName(charset))

  fun stringBody(charset: Charset = StandardCharsets.UTF_8): EndpointIO.StringBody<String> =
    EndpointIO.StringBody(charset, Codec.string, EndpointIO.Info.empty())

  val htmlBodyUtf8: EndpointIO.StringBody<String> =
    EndpointIO.StringBody(
      StandardCharsets.UTF_8,
      Codec.string.format(CodecFormat.TextHtml),
      EndpointIO.Info.empty()
    )

  fun <A> plainBody(
    codec: PlainCodec<A>,
    charset: Charset = StandardCharsets.UTF_8
  ): EndpointIO.StringBody<A> =
    EndpointIO.StringBody(charset, codec, EndpointIO.Info.empty())

  /** A body in any format, read using the given `codec`, from a raw string read using `charset`.*/
  fun <A, CF : CodecFormat> anyFromStringBody(
    codec: Codec<String, A, CF>,
    charset: Charset = StandardCharsets.UTF_8
  ): EndpointIO.StringBody<A> =
    EndpointIO.StringBody(charset, codec, EndpointIO.Info.empty())

  /**
   * Json codecs are usually derived from json-library-specific implicits. That's why integrations with
   * various json libraries define `jsonBody` methods, which directly require the library-specific implicits.
   *
   * If you have a custom json codec, you should use this method instead.
   */
  fun <A> anyJsonBody(codec: JsonCodec<A>): EndpointIO.StringBody<A> =
    anyFromStringBody(codec)

  /** Implement your own xml codec using `Codec.xml()` before using this method.
   */
  fun <A> xmlBody(codec: XmlCodec<A>): EndpointIO.StringBody<A> =
    anyFromStringBody(codec)

  fun byteArrayBody(): EndpointIO.ByteArrayBody<ByteArray> =
    EndpointIO.ByteArrayBody(Codec.byteArray, EndpointIO.Info.empty())

  fun byteBufferBody(): EndpointIO.ByteBufferBody<ByteBuffer> =
    EndpointIO.ByteBufferBody(Codec.byteBuffer, EndpointIO.Info.empty())

  fun inputStreamBody() = EndpointIO.InputStreamBody(Codec.inputStream, EndpointIO.Info.empty())

  fun <A> formBody(codec: Codec<String, A, CodecFormat.XWwwFormUrlencoded>): EndpointIO.StringBody<A> =
    anyFromStringBody(codec)

  fun <A> formBody(charset: Charset, codec: Codec<String, A, CodecFormat.XWwwFormUrlencoded>): EndpointIO.StringBody<A> =
    anyFromStringBody(codec, charset)

  fun method(m: Method): EndpointInput.FixedMethod<Unit> =
    EndpointInput.FixedMethod(m, Codec.idPlain(), EndpointIO.Info.empty())

  fun statusCode(): EndpointOutput.StatusCode<StatusCode> =
    EndpointOutput.StatusCode(emptyMap(), Codec.idPlain(), EndpointIO.Info.empty())

  fun statusCode(statusCode: StatusCode): EndpointOutput.FixedStatusCode<Unit> =
    EndpointOutput.FixedStatusCode(statusCode, Codec.idPlain(), EndpointIO.Info.empty())
}
