package com.fortysevendegrees.tapir

import arrow.core.Tuple4
import arrow.core.Tuple5
import com.fortysevendegrees.tapir.model.CodecFormat
import com.fortysevendegrees.tapir.model.Method

// Elements that can occur as Input
// Such as Query, PathCapture, Cookie, etc
sealed interface EndpointInput<A> : EndpointTransput<A> {

  // Marker for EndpointInput with single output
  sealed interface Single<A> : EndpointInput<A>
  sealed interface Basic<L, A, CF: CodecFormat> : Single<A>, EndpointTransput.Basic<L, A, CF> {

    override fun <B> copyWith(c: Codec<L, B, CF>, i: EndpointIO.Info<B>): Basic<L, B, CF>

    override fun <B> map(mapping: Mapping<A, B>): Basic<L, B, CF> = copyWith(codec.map(mapping), info.map(mapping))
    override fun schema(s: Schema<A>?): EndpointInput.Basic<L, A, CF> = copyWith(codec.schema(s), info)
    override fun modifySchema(modify: (Schema<A>) -> Schema<A>): EndpointInput.Basic<L, A, CF> = copyWith(codec.modifySchema(modify), info)
    override fun description(d: String): EndpointInput.Basic<L, A, CF> = copyWith(codec, info.description(d))
    override fun default(d: A): EndpointInput.Basic<L, A, CF> = copyWith(codec.modifySchema { it.default(d, codec.encode(d)) }, info)
    override fun example(t: A): EndpointInput.Basic<L, A, CF> = copyWith(codec, info.example(t))
    override fun example(example: EndpointIO.Info.Example<A>): EndpointInput.Basic<L, A, CF> = copyWith(codec, info.example(example))
    override fun examples(examples: List<EndpointIO.Info.Example<A>>): EndpointInput.Basic<L, A, CF> = copyWith(codec, info.examples(examples))
    override fun deprecated(): EndpointInput.Basic<L, A, CF> = copyWith(codec, info.deprecated(true))
  }

  data class Query<A>(
    val name: String,
    override val codec: Codec<List<String>, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A>
  ) : Basic<List<String>, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<List<String>, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): Query<B> = Query(name, c, i)
    override fun toString(): String = addValidatorShow("?$name", codec.validator())
  }

  data class QueryParams<A>(
    override val codec: Codec<com.fortysevendegrees.tapir.model.QueryParams, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A>
  ) : Basic<com.fortysevendegrees.tapir.model.QueryParams, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<com.fortysevendegrees.tapir.model.QueryParams, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): QueryParams<B> = QueryParams(c, i)
    override fun toString(): String = "?..."
  }

  data class FixedMethod<A> /* always Unit */(
    val m: Method,
    override val codec: Codec<Unit, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A>
  ) : Basic<Unit, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<Unit, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): FixedMethod<B> = FixedMethod(m, c, i)
    override fun toString(): String = m.value
  }

  data class FixedPath<A> /* always Unit */(
    val s: String,
    override val codec: Codec<Unit, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A>
  ) : Basic<Unit, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<Unit, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): FixedPath<B> = FixedPath(s, c, i)
    override fun toString(): String = "/$s"
  }

  data class PathCapture<A>(
    val name: String?,
    override val codec: PlainCodec<A>,
    override val info: EndpointIO.Info<A>
  ) : Basic<String, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<String, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): PathCapture<B> = PathCapture(name, c, i)
    fun name(n: String): PathCapture<A> = copy(name = n)
    override fun toString(): String = addValidatorShow("/[${name ?: ""}]", codec.validator())
  }

  data class PathsCapture<A>(
    override val codec: Codec<List<String>, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A>
  ) : Basic<List<String>, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<List<String>, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): PathsCapture<B> = PathsCapture(c, i)
    override fun toString(): String = "/..."
  }

  data class Cookie<A>(
    val name: String,
    override val codec: Codec<String?, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A>
  ) : Basic<String?, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<String?, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): Cookie<B> = Cookie(name, c, i)
    override fun toString(): String = addValidatorShow("{cookie $name}", codec.validator())
  }

  data class MappedPair<A, B, C, D>(val input: Pair<A, B, C>, val mapping: Mapping<C, D>) : Single<D> {
    override fun <E> map(m: Mapping<D, E>): MappedPair<A, B, C, E> = MappedPair(input, mapping.map(m))
    override fun toString(): String = input.toString()
  }

  data class Pair<A, B, C>(
    override val first: EndpointInput<A>,
    override val second: EndpointInput<B>,
    override val combine: CombineParams,
    override val split: SplitParams
  ) : EndpointInput<C>, EndpointTransput.Pair<C> {
    override fun <D> map(mapping: Mapping<C, D>): EndpointInput<D> = MappedPair(this, mapping)
    override fun toString(): String = "EndpointInput.Pair($first, $second)"
  }

  fun <A> traverseInputs(isDefinedAt: (EndpointInput<*>) -> Boolean, handle: (EndpointInput<*>) -> List<A>): List<A> =
    when {
      isDefinedAt(this) -> handle(this)
      this is Pair<*, *, *> -> first.traverseInputs(isDefinedAt, handle) + second.traverseInputs(isDefinedAt, handle)
      this is EndpointIO.Pair<*, *, *> -> first.traverseInputs(isDefinedAt, handle) + second.traverseInputs(
        isDefinedAt,
        handle
      )
      this is MappedPair<*, *, *, *> -> input.traverseInputs(isDefinedAt, handle)
      this is EndpointIO.MappedPair<*, *, *, *> -> wrapped.traverseInputs(isDefinedAt, handle)
      // is EndpointInput.Auth<*> -> input.traverseInputs(isDefinedAt, handle)
      else -> emptyList()
    }

  fun asListOfBasicInputs(includeAuth: Boolean = true): List<Basic<*, * ,* >> =
    traverseInputs({ it is Basic<*, *, *> /* || it is EndpointInput.Auth */ }) {
      when (it) {
        is Basic<*, *, *> -> listOf(it)
        // is EndpointInput.Auth<*>  -> if (includeAuth) it.input.asVectorOfBasicInputs(includeAuth) else emptyList()
        else -> throw IllegalStateException("")
      }
    }
}

// We need to support this Arity-22
@JvmName("and")
fun <A, B> EndpointInput<A>.and(other: EndpointInput<B>): EndpointInput<Pair<A, B>> =
  EndpointInput.Pair(this, other,
    { p1, p2 -> Params.ParamsAsList(listOf(p1.asAny, p2.asAny)) },
    { p ->
      Pair(
        Params.ParamsAsAny(p.asList.take(1)),
        Params.ParamsAsAny(p.asList.last())
      )
    }
  )

@JvmName("andLeftUnit")
fun <A> EndpointInput<Unit>.and(other: EndpointInput<A>, dummy: Unit = Unit): EndpointInput<A> =
  EndpointInput.Pair(this, other,
    { _, p2 -> p2 },
    { p -> Pair(Params.Unit, p) }
  )

@JvmName("and2")
fun <A, B, C> EndpointInput<Pair<A, B>>.and(other: EndpointInput<C>): EndpointInput<Triple<A, B, C>> =
  EndpointInput.Pair(this, other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(2)),
        Params.ParamsAsAny(p.asList.takeLast(1))
      )
    }
  )

@JvmName("and2Pair")
fun <A, B, C, D> EndpointInput<Pair<A, B>>.and(other: EndpointInput<Pair<C, D>>): EndpointInput<Tuple4<A, B, C, D>> =
  EndpointInput.Pair(this, other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asList) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(2)),
        Params.ParamsAsList(p.asList.takeLast(2))
      )
    }
  )

@JvmName("and2Unit")
fun <A, B> EndpointInput<Pair<A, B>>.and(other: EndpointInput<Unit>): EndpointInput<Pair<A, B>> =
  EndpointInput.Pair(this, other,
    { p1, _ -> p1 },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(2)),
        Params.Unit
      )
    }
  )

@JvmName("and4")
fun <A, B, C, D> EndpointInput<Triple<A, B, C>>.and(other: EndpointInput<D>): EndpointInput<Tuple4<A, B, C, D>> =
  EndpointInput.Pair(this, other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(3)),
        Params.ParamsAsAny(p.asList.takeLast(1))
      )
    }
  )

@JvmName("and5")
fun <A, B, C, D, E> EndpointInput<Tuple4<A, B, C, D>>.and(other: EndpointInput<D>): EndpointInput<Tuple5<A, B, C, D, E>> =
  EndpointInput.Pair(this, other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(4)),
        Params.ParamsAsAny(p.asList.takeLast(1))
      )
    }
  )