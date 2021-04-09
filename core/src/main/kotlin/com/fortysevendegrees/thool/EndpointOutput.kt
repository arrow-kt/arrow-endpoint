package com.fortysevendegrees.thool

import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.StatusCode as MStatusCode

// Elements that can occur as Output
// Such as StatusCode, Void, etc
sealed interface EndpointOutput<A> : EndpointTransput<A> {

  sealed interface Single<A> : EndpointOutput<A>

  sealed interface Basic<L, A, CF : CodecFormat> : Single<A>, EndpointTransput.Basic<L, A, CF> {
    override fun <B> copyWith(c: Codec<L, B, CF>, i: EndpointIO.Info<B>): Basic<L, B, CF>

    override fun <B> map(mapping: Mapping<A, B>): Basic<L, B, CF> = copyWith(codec.map(mapping), info.map(mapping))
    override fun schema(s: Schema<A>?): Basic<L, A, CF> = copyWith(codec.schema(s), info)
    override fun modifySchema(modify: (Schema<A>) -> Schema<A>): Basic<L, A, CF> =
      copyWith(codec.modifySchema(modify), info)

    override fun description(d: String): Basic<L, A, CF> = copyWith(codec, info.description(d))
    override fun default(d: A): Basic<L, A, CF> = copyWith(codec.modifySchema { it.default(d, codec.encode(d)) }, info)
    override fun example(t: A): Basic<L, A, CF> = copyWith(codec, info.example(t))
    override fun example(example: EndpointIO.Info.Example<A>): Basic<L, A, CF> = copyWith(codec, info.example(example))
    override fun examples(examples: List<EndpointIO.Info.Example<A>>): Basic<L, A, CF> =
      copyWith(codec, info.examples(examples))

    override fun deprecated(): Basic<L, A, CF> = copyWith(codec, info.deprecated(true))
  }

  data class StatusCode<A>(
    val documentedCodes: Map<MStatusCode, EndpointIO.Info<Unit>>,
    override val codec: Codec<MStatusCode, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A>
  ) : Basic<MStatusCode, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<MStatusCode, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): StatusCode<B> =
      StatusCode(documentedCodes, c, i)

    override fun toString(): String = "status code - possible codes ($documentedCodes)"
    fun description(code: MStatusCode, d: String): StatusCode<A> {
      val updatedCodes = documentedCodes + Pair(code, EndpointIO.Info.empty<Unit>().description(d))
      return copy(documentedCodes = updatedCodes)
    }
  }

  data class FixedStatusCode<A>(
    val statusCode: MStatusCode,
    override val codec: Codec<Unit, A, CodecFormat.TextPlain>,
    override val info: EndpointIO.Info<A>
  ) : Basic<Unit, A, CodecFormat.TextPlain> {
    override fun <B> copyWith(
      c: Codec<Unit, B, CodecFormat.TextPlain>,
      i: EndpointIO.Info<B>
    ): FixedStatusCode<B> =
      FixedStatusCode(statusCode, c, i)

    override fun toString(): String = "status code ($statusCode)"
  }

  /**
   * Specifies that for `statusCode`, the given `output` should be used.
   * The `appliesTo` function should determine, whether a runtime value matches the type `O`.
   * This check cannot be in general done by checking the run-time class of the value, due to type erasure (if `O` has
   * type parameters).
   */
  data class StatusMapping<O> internal constructor(
    val statusCode: MStatusCode?,
    val output: EndpointOutput<O>,
    val appliesTo: (Any?) -> Boolean
  )

  class Void<A> : EndpointOutput<A> {
    override fun <B> map(mapping: Mapping<A, B>): Void<B> = Void()
    override fun toString(): String = "void"
  }

  data class MappedPair<A, B, C, D>(val output: Pair<A, B, C>, val mapping: Mapping<C, D>) : Single<D> {
    override fun <E> map(m: Mapping<D, E>): EndpointTransput<E> = MappedPair(output, mapping.map(m))
    override fun toString(): String = output.toString()
  }

  data class Pair<A, B, C>(
    override val first: EndpointOutput<A>,
    override val second: EndpointOutput<B>,
    override val combine: CombineParams,
    override val split: SplitParams
  ) : EndpointOutput<C>, EndpointTransput.Pair<C> {
    override fun <D> map(mapping: Mapping<C, D>): EndpointOutput<D> = MappedPair(this, mapping)
    override fun toString(): String = "EndpointOutput.Pair($first, $second)"
  }

  fun <A> traverseOutputs(isDefinedAt: (EndpointOutput<*>) -> Boolean, handle: (EndpointOutput<*>) -> List<A>): List<A> =
    when {
      isDefinedAt(this) -> handle(this)
      this is EndpointOutput.Pair<*, *, *> -> first.traverseOutputs(isDefinedAt, handle) + second.traverseOutputs(
        isDefinedAt,
        handle
      )
      this is EndpointIO.Pair<*, *, *> -> first.traverseOutputs(isDefinedAt, handle) + second.traverseOutputs(
        isDefinedAt,
        handle
      )
      this is EndpointOutput.MappedPair<*, *, *, *> -> output.traverseOutputs(isDefinedAt, handle)
      this is EndpointIO.MappedPair<*, *, *, *> -> wrapped.traverseOutputs(isDefinedAt, handle)
//    this is EndpointOutput.OneOf<*, *> -> s.mappings.toList().flatMap { ut.output.traverseOutputs(handle) }
      else -> emptyList()
    }
}

// We need to support this Arity-22
@JvmName("and")
fun <A, B> EndpointOutput<A>.and(other: EndpointOutput<B>): EndpointOutput<Pair<A, B>> =
  EndpointOutput.Pair(
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
fun <A> EndpointOutput<Unit>.and(other: EndpointOutput<A>, dummy: Unit = Unit): EndpointOutput<A> =
  EndpointOutput.Pair(
    this,
    other,
    { _, p2 -> p2 },
    { p -> Pair(Params.Unit, p) }
  )

@JvmName("andRightUnit")
fun <A> EndpointOutput<A>.and(other: EndpointOutput<Unit>): EndpointOutput<A> =
  EndpointOutput.Pair(
    this,
    other,
    { p1, _ -> p1 },
    { p -> Pair(p, Params.Unit) }
  )

@JvmName("and3")
fun <A, B, C> EndpointOutput<Pair<A, B>>.and(other: EndpointOutput<C>): EndpointOutput<Triple<A, B, C>> =
  EndpointOutput.Pair(
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
