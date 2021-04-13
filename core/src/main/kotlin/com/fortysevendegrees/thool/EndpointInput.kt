package com.fortysevendegrees.thool

import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple6
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.Method

// Elements that can occur as Input
// Such as Query, PathCapture, Cookie, etc
sealed interface EndpointInput<A> : EndpointTransput<A> {

  // Marker for EndpointInput with single output
  sealed interface Single<A> : EndpointInput<A>
  sealed interface Basic<L, A, CF : CodecFormat> : Single<A>, EndpointTransput.Basic<L, A, CF> {

    override fun <B> copyWith(c: Codec<L, B, CF>, i: EndpointIO.Info<B>): Basic<L, B, CF>

    override fun <B> map(mapping: Mapping<A, B>): Basic<L, B, CF> = copyWith(codec.map(mapping), info.map(mapping))
    override fun schema(s: Schema<A>?): EndpointInput.Basic<L, A, CF> = copyWith(codec.schema(s), info)
    override fun modifySchema(modify: (Schema<A>) -> Schema<A>): EndpointInput.Basic<L, A, CF> =
      copyWith(codec.modifySchema(modify), info)

    override fun description(d: String): EndpointInput.Basic<L, A, CF> = copyWith(codec, info.description(d))
    override fun default(d: A): EndpointInput.Basic<L, A, CF> =
      copyWith(codec.modifySchema { it.default(d, codec.encode(d)) }, info)

    override fun example(t: A): EndpointInput.Basic<L, A, CF> = copyWith(codec, info.example(t))
    override fun example(example: EndpointIO.Info.Example<A>): EndpointInput.Basic<L, A, CF> =
      copyWith(codec, info.example(example))

    override fun examples(examples: List<EndpointIO.Info.Example<A>>): EndpointInput.Basic<L, A, CF> =
      copyWith(codec, info.examples(examples))

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

    override fun toString(): String = "?$name"
  }

  data class QueryParams<A>(
    override val codec: Codec<com.fortysevendegrees.thool.model.QueryParams, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A>
  ) : Basic<com.fortysevendegrees.thool.model.QueryParams, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<com.fortysevendegrees.thool.model.QueryParams, B, CodecFormat.TextPlain>,
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
    override fun toString(): String = "/[${name ?: ""}]"
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

    override fun toString(): String = "{cookie $name}"
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

  companion object {
    fun empty(): EndpointIO.Empty<Unit> =
      EndpointIO.Empty(Codec.idPlain(), EndpointIO.Info.empty())
  }
}

fun <A, B> EndpointInput<A>.reduce(
  ifBody: (EndpointIO.Body<Any?, Any?>) -> List<B> = { emptyList() },
  ifEmpty: (EndpointIO.Empty<Any?>) -> List<B> = { emptyList() },
  ifHeader: (EndpointIO.Header<Any?>) -> List<B> = { emptyList() },
  ifStreamBody: (EndpointIO.StreamBody<Any?>) -> List<B> = { emptyList() },
  ifCookie: (EndpointInput.Cookie<Any?>) -> List<B> = { emptyList() },
  ifFixedMethod: (EndpointInput.FixedMethod<Any?>) -> List<B> = { emptyList() },
  ifFixedPath: (EndpointInput.FixedPath<Any?>) -> List<B> = { emptyList() },
  ifPathCapture: (EndpointInput.PathCapture<Any?>) -> List<B> = { emptyList() },
  ifPathsCapture: (EndpointInput.PathsCapture<Any?>) -> List<B> = { emptyList() },
  ifQuery: (EndpointInput.Query<Any?>) -> List<B> = { emptyList() },
  ifQueryParams: (EndpointInput.QueryParams<Any?>) -> List<B> = { emptyList() },
): List<B> =
  when (this) {
    is EndpointIO.Body<*, *> -> ifBody(this as EndpointIO.Body<Any?, Any?>)
    is EndpointIO.Empty -> ifEmpty(this as EndpointIO.Empty<Any?>)
    is EndpointIO.Header -> ifHeader(this as EndpointIO.Header<Any?>)
    is EndpointIO.StreamBody -> ifStreamBody(this as EndpointIO.StreamBody<Any?>)
    is EndpointInput.Cookie -> ifCookie(this as EndpointInput.Cookie<Any?>)
    is EndpointInput.FixedMethod -> ifFixedMethod(this as EndpointInput.FixedMethod<Any?>)
    is EndpointInput.FixedPath -> ifFixedPath(this as EndpointInput.FixedPath<Any?>)
    is EndpointInput.PathCapture -> ifPathCapture(this as EndpointInput.PathCapture<Any?>)
    is EndpointInput.PathsCapture -> ifPathsCapture(this as EndpointInput.PathsCapture<Any?>)
    is EndpointInput.Query -> ifQuery(this as EndpointInput.Query<Any?>)
    is EndpointInput.QueryParams -> ifQueryParams(this as EndpointInput.QueryParams<Any?>)

    is EndpointInput.Pair<*, *, *> ->
      first.reduce(
        ifBody,
        ifEmpty,
        ifHeader,
        ifStreamBody,
        ifCookie,
        ifFixedMethod,
        ifFixedPath,
        ifPathCapture,
        ifPathsCapture,
        ifQuery,
        ifQueryParams
      ) +
        second.reduce(
          ifBody,
          ifEmpty,
          ifHeader,
          ifStreamBody,
          ifCookie,
          ifFixedMethod,
          ifFixedPath,
          ifPathCapture,
          ifPathsCapture,
          ifQuery,
          ifQueryParams
        )
    is EndpointIO.Pair<*, *, *> ->
      first.reduce(
        ifBody,
        ifEmpty,
        ifHeader,
        ifStreamBody,
        ifCookie,
        ifFixedMethod,
        ifFixedPath,
        ifPathCapture,
        ifPathsCapture,
        ifQuery,
        ifQueryParams
      ) +
        second.reduce(
          ifBody,
          ifEmpty,
          ifHeader,
          ifStreamBody,
          ifCookie,
          ifFixedMethod,
          ifFixedPath,
          ifPathCapture,
          ifPathsCapture,
          ifQuery,
          ifQueryParams
        )
    is EndpointIO.MappedPair<*, *, *, *> ->
      wrapped.first.reduce(
        ifBody,
        ifEmpty,
        ifHeader,
        ifStreamBody,
        ifCookie,
        ifFixedMethod,
        ifFixedPath,
        ifPathCapture,
        ifPathsCapture,
        ifQuery,
        ifQueryParams
      ) +
        wrapped.second.reduce(
          ifBody,
          ifEmpty,
          ifHeader,
          ifStreamBody,
          ifCookie,
          ifFixedMethod,
          ifFixedPath,
          ifPathCapture,
          ifPathsCapture,
          ifQuery,
          ifQueryParams
        )
    is EndpointInput.MappedPair<*, *, *, *> ->
      input.first.reduce(
        ifBody,
        ifEmpty,
        ifHeader,
        ifStreamBody,
        ifCookie,
        ifFixedMethod,
        ifFixedPath,
        ifPathCapture,
        ifPathsCapture,
        ifQuery,
        ifQueryParams
      ) +
        input.second.reduce(
          ifBody,
          ifEmpty,
          ifHeader,
          ifStreamBody,
          ifCookie,
          ifFixedMethod,
          ifFixedPath,
          ifPathCapture,
          ifPathsCapture,
          ifQuery,
          ifQueryParams
        )
  }

fun <A> EndpointInput<A>.toList(): List<EndpointInput<Any?>> =
  reduce(::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf)

fun <A> EndpointInput<A>.asListOfBasicInputs(includeAuth: Boolean = true): List<EndpointInput.Basic<*, *, *>> =
  toList().mapNotNull {
//      if(includeAuth) it as? Basic<*, *, *> ?: it as EndpointInput.Auth<*> else
    it as? EndpointInput.Basic<*, *, *>
  }

fun <A> EndpointInput<A>.method(): Method? =
  toList().mapNotNull { (it as? EndpointInput.FixedMethod<*>)?.m }
    .firstOrNull()

//  fun auth(): Method? =
//    toList().mapNotNull { (it as? EndpointInput.Auth<*>)?.m }
//      .firstOrNull()

// We need to support this Arity-22
@JvmName("and")
fun <A, B> EndpointInput<A>.and(other: EndpointInput<B>): EndpointInput<Pair<A, B>> =
  EndpointInput.Pair(
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

@JvmName("andLeftUnit")
fun <A> EndpointInput<Unit>.and(other: EndpointInput<A>, dummy: Unit = Unit): EndpointInput<A> =
  EndpointInput.Pair(
    this,
    other,
    { _, p2 -> p2 },
    { p -> Pair(Params.Unit, p) }
  )

@JvmName("andRightUnit")
fun <A> EndpointInput<A>.and(other: EndpointInput<Unit>, dummy: Unit = Unit): EndpointInput<A> =
  EndpointInput.Pair(
    this,
    other,
    { p1, _ -> p1 },
    { p -> Pair(p, Params.Unit) }
  )

@JvmName("andLeftRightUnit")
fun EndpointInput<Unit>.and(other: EndpointInput<Unit>, dummy: Unit = Unit): EndpointInput<Unit> =
  EndpointInput.Pair(
    this,
    other,
    { p1, _ -> p1 },
    { p -> Pair(p, Params.Unit) }
  )

@JvmName("and2")
fun <A, B, C> EndpointInput<Pair<A, B>>.and(other: EndpointInput<C>): EndpointInput<Triple<A, B, C>> =
  EndpointInput.Pair(
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
fun <A, B, C, D> EndpointInput<Pair<A, B>>.and(other: EndpointInput<Pair<C, D>>): EndpointInput<Tuple4<A, B, C, D>> =
  EndpointInput.Pair(
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

@JvmName("and2Unit")
fun <A, B> EndpointInput<Pair<A, B>>.and(other: EndpointInput<Unit>): EndpointInput<Pair<A, B>> =
  EndpointInput.Pair(
    this,
    other,
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
  EndpointInput.Pair(
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
fun <A, B, C, D, E> EndpointInput<Tuple4<A, B, C, D>>.and(other: EndpointInput<E>): EndpointInput<Tuple5<A, B, C, D, E>> =
  EndpointInput.Pair(
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

@JvmName("and6")
fun <A, B, C, D, E, F> EndpointInput<Tuple5<A, B, C, D, E>>.and(other: EndpointInput<F>): EndpointInput<Tuple6<A, B, C, D, E, F>> =
  EndpointInput.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(5)),
        Params.ParamsAsAny(p.asList.takeLast(1))
      )
    }
  )
