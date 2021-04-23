package com.fortysevendegrees.thool

import arrow.core.Tuple4
import arrow.core.Tuple5
import com.fortysevendegrees.thool.model.CodecFormat
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

// Elements that can occur in both input and output
// Such as body, headers, etc
public sealed interface EndpointIO<A> : EndpointInput<A>, EndpointOutput<A> {

  public sealed interface Single<A> : EndpointIO<A>, EndpointInput.Single<A>, EndpointOutput.Single<A>

  public sealed interface Basic<L, A, CF : CodecFormat> :
    Single<A>,
    EndpointInput.Basic<L, A, CF>,
    EndpointOutput.Basic<L, A, CF> {
    override fun <B> copyWith(c: Codec<L, B, CF>, i: Info<B>): Basic<L, B, CF>

    override fun <B> map(mapping: Mapping<A, B>): Basic<L, B, CF> = copyWith(codec.map(mapping), info.map(mapping))
    override fun schema(s: Schema<A>?): Basic<L, A, CF> = copyWith(codec.schema(s), info)
    override fun modifySchema(modify: (Schema<A>) -> Schema<A>): Basic<L, A, CF> =
      copyWith(codec.modifySchema(modify), info)

    override fun description(d: String): Basic<L, A, CF> = copyWith(codec, info.description(d))
    override fun default(d: A): Basic<L, A, CF> = copyWith(codec.modifySchema { it.default(d, codec.encode(d)) }, info)
    override fun example(t: A): Basic<L, A, CF> = copyWith(codec, info.example(t))
    override fun example(example: Info.Example<A>): Basic<L, A, CF> = copyWith(codec, info.example(example))
    override fun examples(examples: List<Info.Example<A>>): Basic<L, A, CF> = copyWith(codec, info.examples(examples))
    override fun deprecated(): Basic<L, A, CF> = copyWith(codec, info.deprecated(true))
  }

  public data class Empty<A>(override val codec: Codec<Unit, A, CodecFormat.TextPlain>, override val info: Info<A>) :
    Basic<Unit, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<Unit, B, CodecFormat.TextPlain>,
      i: Info<B>
    ): Empty<B> = Empty(c, i)

    override fun toString(): String = "-"
  }

  public data class Header<A>(
    val name: String,
    override val codec: Codec<List<String>, A, CodecFormat.TextPlain>,
    override val info: Info<A>
  ) : Basic<List<String>, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<List<String>, B, CodecFormat.TextPlain>,
      i: Info<B>
    ): Header<B> = Header(name, c, i)

    override fun toString(): String = "{header $name}"
  }

  // TODO FileBody, MultipartBody
  public sealed interface Body<R, T> : Basic<R, T, CodecFormat>
  public sealed interface BinaryBody<R, T> : Body<R, T>

  public data class StringBody<T>(
    val charset: Charset,
    override val codec: Codec<String, T, CodecFormat>,
    override val info: Info<T>
  ) : Basic<String, T, CodecFormat>, Body<String, T> {
    override fun <B> copyWith(c: Codec<String, B, CodecFormat>, i: Info<B>): StringBody<B> =
      StringBody(charset, c, i)

    override fun toString(): String {
      val format = codec.format.mediaType
      return "{body as $format$charset}"
    }
  }

  public data class ByteArrayBody<T>(
    override val codec: Codec<ByteArray, T, CodecFormat>,
    override val info: Info<T>
  ) : Basic<ByteArray, T, CodecFormat>, BinaryBody<ByteArray, T> {
    override fun <B> copyWith(c: Codec<ByteArray, B, CodecFormat>, i: Info<B>): ByteArrayBody<B> =
      ByteArrayBody(c, i)

    override fun toString(): String {
      val format = codec.format.mediaType
      return "{body as $format}"
    }
  }

  public data class ByteBufferBody<T>(
    override val codec: Codec<ByteBuffer, T, CodecFormat>,
    override val info: Info<T>
  ) : Basic<ByteBuffer, T, CodecFormat>, BinaryBody<ByteBuffer, T> {
    override fun <B> copyWith(c: Codec<ByteBuffer, B, CodecFormat>, i: Info<B>): ByteBufferBody<B> =
      ByteBufferBody(c, i)

    override fun toString(): String {
      val format = codec.format.mediaType
      return "{body as $format}"
    }
  }

  public data class InputStreamBody<T>(
    override val codec: Codec<InputStream, T, CodecFormat>,
    override val info: Info<T>
  ) : Basic<InputStream, T, CodecFormat>, BinaryBody<InputStream, T> {
    override fun <B> copyWith(c: Codec<InputStream, B, CodecFormat>, i: Info<B>): InputStreamBody<B> =
      InputStreamBody(c, i)

    override fun toString(): String {
      val format = codec.format.mediaType
      return "{body as $format}"
    }
  }

  public data class StreamBody<A>(
    override val codec: Codec<Flow<Byte>, A, CodecFormat>,
    override val info: Info<A>,
    val charset: Charset?
  ) : Basic<Flow<Byte>, A, CodecFormat>, EndpointTransput.Basic<Flow<Byte>, A, CodecFormat> {
    override fun <B> copyWith(
      c: Codec<Flow<Byte>, B, CodecFormat>,
      i: Info<B>
    ): StreamBody<B> = StreamBody(c, i, charset)

    override fun toString(): String = "{body as stream}"
  }

  public data class Info<T>(val description: String?, val examples: List<Example<T>>, val deprecated: Boolean) {
    public fun description(d: String): Info<T> = copy(description = d)
    public fun example(): T? = examples.firstOrNull()?.value
    public fun example(t: T): Info<T> = example(Example(t))
    public fun example(example: Example<T>): Info<T> = copy(examples = examples + example)
    public fun examples(ts: List<Example<T>>): Info<T> = copy(examples = ts)
    public fun deprecated(d: Boolean): Info<T> = copy(deprecated = d)

    public fun <U> map(codec: Mapping<T, U>): Info<U> =
      Info(
        description,
        examples.mapNotNull { e ->
          val result = codec.decode(e.value)
          if (result is DecodeResult.Value) Example(result.value, e.name, e.summary)
          else null
        },
        deprecated
      )

    public data class Example<out A>(val value: A, val name: String? = null, val summary: String? = null) {
      public fun <B> map(transform: (A) -> B): Example<B> =
        Example(transform(value), name, summary)
    }

    public companion object {
      public fun <A> empty(): Info<A> = Info(null, emptyList(), deprecated = false)
    }
  }

  public data class MappedPair<A, B, C, D>(val wrapped: Pair<A, B, C>, val mapping: Mapping<C, D>) : Single<D> {
    override fun <E> map(m: Mapping<D, E>): MappedPair<A, B, C, E> = MappedPair(wrapped, mapping.map(m))
    override fun toString(): String = wrapped.toString()
  }

  public data class Pair<A, B, C>(
    override val first: EndpointIO<A>,
    override val second: EndpointIO<B>,
    override val combine: CombineParams,
    override val split: SplitParams
  ) : EndpointIO<C>, EndpointTransput.Pair<C> {
    override fun <D> map(mapping: Mapping<C, D>): MappedPair<A, B, C, D> = MappedPair(this, mapping)
    override fun toString(): String = "EndpointIO.Pair($first, $second)"
  }
}

// We need to support this Arity-22
@JvmName("and")
public fun <A, B> EndpointIO<A>.and(other: EndpointIO<B>): EndpointIO<Pair<A, B>> =
  EndpointIO.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(listOf(p1.asAny, p2.asAny)) },
    { p ->
      Pair(
        Params.ParamsAsAny(p.asList.take(1)),
        Params.ParamsAsAny(p.asList.last())
      )
    }
  )

@JvmName("andRightUnit")
public fun <A> EndpointIO<A>.and(other: EndpointIO<Unit>): EndpointIO<A> =
  EndpointIO.Pair(
    this,
    other,
    { p1, _ -> p1 },
    { p -> Pair(p, Params.Unit) }
  )

@JvmName("andLeftUnit")
public fun <A> EndpointIO<Unit>.and(other: EndpointIO<A>, dummy: Unit = Unit): EndpointIO<A> =
  EndpointIO.Pair(
    this,
    other,
    { _, p2 -> p2 },
    { p -> Pair(Params.Unit, p) }
  )

@JvmName("and3")
public fun <A, B, C> EndpointIO<Pair<A, B>>.and(other: EndpointIO<C>): EndpointIO<Triple<A, B, C>> =
  EndpointIO.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(2)),
        Params.ParamsAsAny(p.asList.takeLast(1))
      )
    }
  )

@JvmName("and2Pair")
public fun <A, B, C, D> EndpointIO<Pair<A, B>>.and(other: EndpointIO<Pair<C, D>>): EndpointIO<Tuple4<A, B, C, D>> =
  EndpointIO.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asList) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(2)),
        Params.ParamsAsList(p.asList.takeLast(2))
      )
    }
  )

@JvmName("and4")
public fun <A, B, C, D> EndpointIO<Triple<A, B, C>>.and(other: EndpointIO<D>): EndpointIO<Tuple4<A, B, C, D>> =
  EndpointIO.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(3)),
        Params.ParamsAsAny(p.asList.takeLast(1))
      )
    }
  )

@JvmName("and5")
public fun <A, B, C, D, E> EndpointIO<Tuple4<A, B, C, D>>.and(other: EndpointIO<D>): EndpointIO<Tuple5<A, B, C, D, E>> =
  EndpointIO.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(4)),
        Params.ParamsAsAny(p.asList.takeLast(1))
      )
    }
  )
