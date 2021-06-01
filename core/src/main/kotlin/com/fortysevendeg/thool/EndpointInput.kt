package com.fortysevendeg.thool

import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple6
import com.fortysevendeg.thool.model.CodecFormat
import com.fortysevendeg.thool.model.Method
import com.fortysevendeg.thool.model.QueryParams as MQueryParams

/**
 * Endpoint Input of [A].
 *
 * All [EndpointInput], except pair, have a [Codec].
 * The [Codec] represents the mapping between the low-level HTTP encoding, and the type [A].
 *
 * ```kotlin
 * data class User(val name: String, val age: Int)
 *
 * val nameParam: EndpointInput.Query<String> =
 *   Endpoint.Query("name", Codec.string)
 *
 * val ageParam: EndpointInput.Query<Int> =
 *   Endpoint.Query("age", Codec.int)
 *
 * val userParam: EndpointInput<User> =
 *   nameParam.and(ageParam)
 *     .map({ (n, a) -> User(n, a) }, { (n, a) -> Pair(n, a) })
 * ```
 *
 * You compose and mix-and-match all [EndpointInput] defined , and the subtypes of [EndpointIO].
 */
public sealed interface EndpointInput<A> : EndpointTransput<A> {

  override fun <B> map(mapping: Mapping<A, B>): EndpointInput<B>
  override fun <B> map(f: (A) -> B, g: (B) -> A): EndpointInput<B> = map(Mapping.from(f, g))
  override fun <B> mapDecode(f: (A) -> DecodeResult<B>, g: (B) -> A): EndpointInput<B> = map(Mapping.fromDecode(f, g))

  public sealed interface Single<A> : EndpointInput<A>
  public sealed interface Basic<L, A, CF : CodecFormat> : Single<A>, EndpointTransput.Basic<L, A, CF> {

    override fun <B> copyWith(c: Codec<L, B, CF>, i: EndpointIO.Info<B>): Basic<L, B, CF>

    override fun <B> map(mapping: Mapping<A, B>): Basic<L, B, CF> = copyWith(codec.map(mapping), info.map(mapping))

    override fun schema(s: Schema<A>?): Basic<L, A, CF> = copyWith(codec.schema(s), info)
    override fun modifySchema(modify: (Schema<A>) -> Schema<A>): Basic<L, A, CF> =
      copyWith(codec.modifySchema(modify), info)

    override fun description(d: String): Basic<L, A, CF> = copyWith(codec, info.description(d))
    override fun default(d: A): Basic<L, A, CF> =
      copyWith(codec.modifySchema { it.default(d, codec.encode(d)) }, info)

    override fun example(t: A): Basic<L, A, CF> = copyWith(codec, info.example(t))
    override fun example(example: EndpointIO.Info.Example<A>): Basic<L, A, CF> =
      copyWith(codec, info.example(example))

    override fun examples(examples: List<EndpointIO.Info.Example<A>>): Basic<L, A, CF> =
      copyWith(codec, info.examples(examples))

    override fun deprecated(): Basic<L, A, CF> = copyWith(codec, info.deprecated(true))
  }

  public data class Query<A>(
    val name: String,
    override val codec: Codec<List<String>, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A> = EndpointIO.Info.empty()
  ) : Basic<List<String>, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<List<String>, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): Query<B> = Query(name, c, i)

    override fun toString(): String = "?$name"
  }

  public data class QueryParams<A>(
    override val codec: Codec<MQueryParams, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A> = EndpointIO.Info.empty()
  ) : Basic<MQueryParams, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<MQueryParams, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): QueryParams<B> = QueryParams(c, i)

    override fun toString(): String = "?..."
  }

  public data class FixedMethod<A> /* always Unit */(
    val m: Method,
    override val codec: Codec<Unit, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A> = EndpointIO.Info.empty()
  ) : Basic<Unit, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<Unit, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): FixedMethod<B> = FixedMethod(m, c, i)

    override fun toString(): String = m.value
  }

  public data class FixedPath<A> /* always Unit */(
    val s: String,
    override val codec: Codec<Unit, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A> = EndpointIO.Info.empty()
  ) : Basic<Unit, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<Unit, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): FixedPath<B> = FixedPath(s, c, i)

    override fun toString(): String = "/$s"
  }

  public data class PathCapture<A>(
    val name: String?,
    override val codec: PlainCodec<A>,
    override val info: EndpointIO.Info<A> = EndpointIO.Info.empty()
  ) : Basic<String, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<String, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): PathCapture<B> = PathCapture(name, c, i)

    public fun name(n: String): PathCapture<A> = copy(name = n)
    override fun toString(): String = "/[${name ?: ""}]"
  }

  public data class PathsCapture<A>(
    override val codec: Codec<List<String>, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A> = EndpointIO.Info.empty()
  ) : Basic<List<String>, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<List<String>, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): PathsCapture<B> = PathsCapture(c, i)

    override fun toString(): String = "/..."
  }

  public data class Cookie<A>(
    val name: String,
    override val codec: Codec<String?, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A> = EndpointIO.Info.empty()
  ) : Basic<String?, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<String?, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): Cookie<B> = Cookie(name, c, i)

    override fun toString(): String = "{cookie $name}"
  }

  public data class MappedPair<A, B, C, D>(val input: Pair<A, B, C>, val mapping: Mapping<C, D>) : Single<D> {
    override fun <E> map(mapping: Mapping<D, E>): MappedPair<A, B, C, E> = MappedPair(input, this.mapping.map(mapping))
    override fun toString(): String = input.toString()
  }

  /** Not a data class (to avoid copy) and an internal constructor since it should only be constructed with [and] */
  public class Pair<A, B, C> internal constructor(
    override val first: EndpointInput<A>,
    override val second: EndpointInput<B>,
    override val combine: CombineParams,
    override val split: SplitParams
  ) : EndpointInput<C>, EndpointTransput.Pair<C> {
    override fun <D> map(mapping: Mapping<C, D>): EndpointInput<D> = MappedPair(this, mapping)
    override fun toString(): String = "EndpointInput.Pair($first, $second)"

    override fun equals(other: Any?): Boolean {
      return when {
        other == null -> false
        this === other -> true
        this::class != other::class -> false
        else -> {
          other as Pair<*, *, *>

          if (first != other.first) return false
          if (second != other.second) return false
          if (combine != other.combine) return false
          if (split != other.split) return false
          true
        }
      }
    }

    override fun hashCode(): Int {
      var result = first.hashCode()
      result = 31 * result + second.hashCode()
      result = 31 * result + combine.hashCode()
      result = 31 * result + split.hashCode()
      return result
    }
  }

  public fun toList(): List<EndpointInput<Any?>> =
    reduce(::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf, ::listOf)

  public fun asListOfBasicInputs(includeAuth: Boolean = true): List<Basic<*, *, *>> =
    toList().mapNotNull {
//      if(includeAuth) it as? Basic<*, *, *> ?: it as EndpointInput.Auth<*> else
      it as? Basic<*, *, *>
    }

  public fun method(): Method? =
    toList().firstNotNull { (it as? FixedMethod<*>)?.m }

  public companion object {
    public fun empty(): EndpointIO.Empty<Unit> =
      EndpointIO.Empty(Codec.idPlain(), EndpointIO.Info.empty())
  }
}

// Small util function that exits-fast to find first value in Iterable
private inline fun <A, B> Iterable<A>.firstNotNull(predicate: (A) -> B?): B? {
  for (element in this) predicate(element)?.let { return@firstNotNull it }
  return null
}

@Suppress("UNCHECKED_CAST")
public fun <A, B> EndpointInput<A>.reduce(
  ifBody: (EndpointIO.Body<Any?, Any?>) -> List<B> = { emptyList() },
  ifEmpty: (EndpointIO.Empty<Any?>) -> List<B> = { emptyList() },
  ifHeader: (EndpointIO.Header<Any?>) -> List<B> = { emptyList() },
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
          ifCookie,
          ifFixedMethod,
          ifFixedPath,
          ifPathCapture,
          ifPathsCapture,
          ifQuery,
          ifQueryParams
        )
  }

// We need to support this Arity-22
@JvmName("and")
public fun <A, B> EndpointInput<A>.and(other: EndpointInput<B>): EndpointInput<Pair<A, B>> =
  EndpointInput.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(listOf(p1.asAny, p2.asAny)) },
    { p ->
      Pair(
        Params.ParamsAsAny(p.asList.first()),
        Params.ParamsAsAny(p.asList.last())
      )
    }
  )

@JvmName("andLeftUnit")
public fun <A> EndpointInput<Unit>.and(other: EndpointInput<A>, @Suppress("UNUSED_PARAMETER") dummy: Unit = Unit): EndpointInput<A> =
  EndpointInput.Pair(
    this,
    other,
    { _, p2 -> p2 },
    { p -> Pair(Params.Unit, p) }
  )

@JvmName("andRightUnit")
public fun <A> EndpointInput<A>.and(other: EndpointInput<Unit>, @Suppress("UNUSED_PARAMETER") dummy: Unit = Unit): EndpointInput<A> =
  EndpointInput.Pair(
    this,
    other,
    { p1, _ -> p1 },
    { p -> Pair(p, Params.Unit) }
  )

@JvmName("andLeftRightUnit")
public fun EndpointInput<Unit>.and(other: EndpointInput<Unit>, @Suppress("UNUSED_PARAMETER") dummy: Unit = Unit): EndpointInput<Unit> =
  EndpointInput.Pair(
    this,
    other,
    { _, p2 -> p2 },
    { p -> Pair(Params.Unit, p) }
  )

@JvmName("and2")
public fun <A, B, C> EndpointInput<Pair<A, B>>.and(other: EndpointInput<C>): EndpointInput<Triple<A, B, C>> =
  EndpointInput.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(2)),
        Params.ParamsAsAny(p.asList.last())
      )
    }
  )

@JvmName("and2Pair")
public fun <A, B, C, D> EndpointInput<Pair<A, B>>.and(other: EndpointInput<Pair<C, D>>): EndpointInput<Tuple4<A, B, C, D>> =
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
public fun <A, B> EndpointInput<Pair<A, B>>.and(other: EndpointInput<Unit>): EndpointInput<Pair<A, B>> =
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

@JvmName("and3")
public fun <A, B, C, D> EndpointInput<Triple<A, B, C>>.and(other: EndpointInput<D>): EndpointInput<Tuple4<A, B, C, D>> =
  EndpointInput.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(3)),
        Params.ParamsAsAny(p.asList.last())
      )
    }
  )

@JvmName("and4")
public fun <A, B, C, D, E> EndpointInput<Tuple4<A, B, C, D>>.and(other: EndpointInput<E>): EndpointInput<Tuple5<A, B, C, D, E>> =
  EndpointInput.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(4)),
        Params.ParamsAsAny(p.asList.last())
      )
    }
  )

@JvmName("and5")
public fun <A, B, C, D, E, F> EndpointInput<Tuple5<A, B, C, D, E>>.and(other: EndpointInput<F>): EndpointInput<Tuple6<A, B, C, D, E, F>> =
  EndpointInput.Pair(
    this,
    other,
    { p1, p2 -> Params.ParamsAsList(p1.asList + p2.asAny) },
    { p ->
      Pair(
        Params.ParamsAsList(p.asList.take(5)),
        Params.ParamsAsAny(p.asList.last())
      )
    }
  )
