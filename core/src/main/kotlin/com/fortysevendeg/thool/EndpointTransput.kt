package com.fortysevendeg.thool

import com.fortysevendeg.thool.model.CodecFormat

/**
 * A transput is either an input, or an output (see: https://ell.stackexchange.com/questions/21405/hypernym-for-input-and-output).
 * The transput interface contain some common functionality, shared by all inputs and outputs.
 *
 * The hierarchy is as follows:
 *
 *       EndpointTransput
 *           /     \
 * EndpointInput  EndpointOutput
 *           \     /
 *         EndpointIO
 */
public sealed interface EndpointTransput<A> {

  public fun <B> map(mapping: Mapping<A, B>): EndpointTransput<B>
  public fun <B> map(f: (A) -> B, g: (B) -> A): EndpointTransput<B> = map(Mapping.from(f, g))
  public fun <B> mapDecode(f: (A) -> DecodeResult<B>, g: (B) -> A): EndpointTransput<B> = map(Mapping.fromDecode(f, g))

  public sealed interface Basic<L, A, CF : CodecFormat> : EndpointTransput<A> {
    public val codec: Codec<L, A, CF>
    public val info: EndpointIO.Info<A>

    public fun <B> copyWith(c: Codec<L, B, CF>, i: EndpointIO.Info<B>): Basic<L, B, CF>

    override fun <B> map(mapping: Mapping<A, B>): Basic<L, B, CF> = copyWith(codec.map(mapping), info.map(mapping))
    public fun schema(s: Schema<A>?): Basic<L, A, CF> = copyWith(codec.schema(s), info)
    public fun modifySchema(modify: (Schema<A>) -> Schema<A>): Basic<L, A, CF> = copyWith(codec.modifySchema(modify), info)
    public fun description(d: String): Basic<L, A, CF> = copyWith(codec, info.description(d))
    public fun default(d: A): Basic<L, A, CF> = copyWith(codec.modifySchema { it.default(d, codec.encode(d)) }, info)
    public fun example(t: A): Basic<L, A, CF> = copyWith(codec, info.example(t))
    public fun example(example: EndpointIO.Info.Example<A>): Basic<L, A, CF> = copyWith(codec, info.example(example))
    public fun examples(examples: List<EndpointIO.Info.Example<A>>): Basic<L, A, CF> = copyWith(codec, info.examples(examples))
    public fun deprecated(): Basic<L, A, CF> = copyWith(codec, info.deprecated(true))
  }

  public sealed interface Pair<A> : EndpointTransput<A> {
    public val first: EndpointTransput<*>
    public val second: EndpointTransput<*>

    public val combine: CombineParams
    public val split: SplitParams
  }
}
