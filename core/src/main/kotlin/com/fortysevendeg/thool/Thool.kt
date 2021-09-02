package com.fortysevendeg.thool

import arrow.core.Either
import arrow.core.prependTo
import com.fortysevendeg.thool.dsl.PathSyntax
import com.fortysevendeg.thool.model.CodecFormat
import com.fortysevendeg.thool.model.Cookie
import com.fortysevendeg.thool.model.Header
import com.fortysevendeg.thool.model.Method
import com.fortysevendeg.thool.model.QueryParams
import com.fortysevendeg.thool.model.StatusCode
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

  public fun <Input> path(path: PathSyntax.() -> EndpointInput<Input>): EndpointInput<Input> =
    path(PathSyntax)

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
    header(Header.Cookie, Codec.cookiesCodec)

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

  /**
   * Maps [StatusCode]s to outputs.
   * All outputs must have a common supertype [A].
   * Typically, the supertype is a sealed class, and the mappings are implementing classes.
   *
   * Note that exhaustiveness of mappings is not checked (that all subtypes of a sealed class are covered).
   *
   * We can use this to for example to define an [EndpointOutput] that returns an [Either] of [Int] or [String].
   * Depending on if it returns [Either.Left] or [Either.Right] it allows you to return a different [StatusCode].
   *
   * The server decided based on the type, which [StatusCode] it needs to return,
   * and the client determines based on the [StatusCode] which [Codec] to use to decode the response.
   *
   * ```kotlin
   * val output: EndpointOutput.OneOf<Either<Int, String>, Either<Int, String>> =
   *   oneOf(
   *     statusMapping(StatusCode.Accepted, plainBody(Codec.int).map({ Either.Left(it) }, { it.value })),
   *     statusMapping(StatusCode.Ok, stringBody().map({ Either.Right(it) }, { it.value }))
   *   )
   * ```
   */
  public fun <A> oneOf(firstCase: EndpointOutput.StatusMapping<A>, vararg otherCases: EndpointOutput.StatusMapping<A>): EndpointOutput.OneOf<A, A> =
    EndpointOutput.OneOf(firstCase prependTo otherCases.toList(), Codec.idPlain())

  /**
   * Create a status mapping which uses [statusCode] and [output] if the outputted value matches the type of [A].
   * Should be used in [oneOf] output descriptions.
   */
  public inline fun <reified A> statusMapping(statusCode: StatusCode, output: EndpointOutput<A>): EndpointOutput.StatusMapping<A> =
    EndpointOutput.StatusMapping(statusCode, output) { a: Any? -> a is A }

  /**
   * Create a status mapping which uses [statusCode] and [output] if the outputted value matches the type of [A].
   * Should be used in [oneOf] output descriptions.
   */
  public fun <A> statusMapping(statusCode: StatusCode, output: EndpointOutput<A>, firstExactValue: A, vararg rest: A): EndpointOutput.StatusMapping<A> {
    val set = setOf(firstExactValue) + rest.toSet()
    return EndpointOutput.StatusMapping(statusCode, output) { a: Any? -> a in set }
  }

  /** Create a fallback mapping to be used in [oneOf] output descriptions */
  public fun <A> statusDefaultMapping(output: EndpointOutput<A>): EndpointOutput.StatusMapping<A> =
    EndpointOutput.StatusMapping(null, output) { true }

  /** Reads authorization data from the given `input`. */
  public fun <A> apiKey(
    input: EndpointInput.Single<A>,
    challenge: EndpointInput.WWWAuthenticate = EndpointInput.WWWAuthenticate.apiKey()
  ): EndpointInput.Auth.ApiKey<A> = EndpointInput.Auth.ApiKey(input, challenge, null)
}
