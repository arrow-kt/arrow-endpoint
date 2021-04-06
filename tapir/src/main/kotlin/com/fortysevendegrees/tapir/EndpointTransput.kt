package com.fortysevendegrees.tapir

import com.fortysevendegrees.thool.model.CodecFormat

/** A transput is EITHER an input, or an output (see: https://ell.stackexchange.com/questions/21405/hypernym-for-input-and-output).
 * The transput traits contain common functionality, shared by all inputs and outputs.
 *
 * Note that implementations of `EndpointIO` can be used BOTH as inputs and outputs.
 *
 * The hierarchy is as follows:
 *
 *                        /---> `EndpointInput`  >---\
 * `EndpointTransput` >---                            ---> `EndpointIO`
 *                        \---> `EndpointOutput` >---/
 */
sealed interface EndpointTransput<A> {

  fun <B> map(mapping: Mapping<A, B>): EndpointTransput<B>
  fun <B> map(f: (A) -> B, g: (B) -> A): EndpointTransput<B> = map(Mapping.from(f, g))
  fun <B> mapDecode(f: (A) -> DecodeResult<B>, g: (B) -> A): EndpointTransput<B> = map(Mapping.fromDecode(f, g))

  fun validate(v: Validator<A>): EndpointTransput<A> = map(Mapping.id<A>().validate(v))

  sealed interface Basic<L, A, CF : CodecFormat> : EndpointTransput<A> {
    val codec: Codec<L, A, CF>
    val info: EndpointIO.Info<A>

    fun <B> copyWith(c: Codec<L, B, CF>, i: EndpointIO.Info<B>): Basic<L, B, CF>

    override fun <B> map(mapping: Mapping<A, B>): Basic<L, B, CF> = copyWith(codec.map(mapping), info.map(mapping))
    fun schema(s: Schema<A>?): Basic<L, A, CF> = copyWith(codec.schema(s), info)
    fun modifySchema(modify: (Schema<A>) -> Schema<A>): Basic<L, A, CF> = copyWith(codec.modifySchema(modify), info)
    fun description(d: String): Basic<L, A, CF> = copyWith(codec, info.description(d))
    fun default(d: A): Basic<L, A, CF> = copyWith(codec.modifySchema { it.default(d, codec.encode(d)) }, info)
    fun example(t: A): Basic<L, A, CF> = copyWith(codec, info.example(t))
    fun example(example: EndpointIO.Info.Example<A>): Basic<L, A, CF> = copyWith(codec, info.example(example))
    fun examples(examples: List<EndpointIO.Info.Example<A>>): Basic<L, A, CF> = copyWith(codec, info.examples(examples))
    fun deprecated(): Basic<L, A, CF> = copyWith(codec, info.deprecated(true))
  }

  sealed interface Pair<A> : EndpointTransput<A> {
    val first: EndpointTransput<*>
    val second: EndpointTransput<*>

    val combine: CombineParams
    val split: SplitParams
  }
}