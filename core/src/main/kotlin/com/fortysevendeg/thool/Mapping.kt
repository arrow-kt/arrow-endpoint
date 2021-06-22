package com.fortysevendeg.thool

import arrow.core.Option
import arrow.core.andThen
import arrow.core.getOrElse
import arrow.core.identity
import arrow.core.nonFatalOrThrow

/**
 * A bi-directional mapping between values of type `L` and values of type `H`.
 *
 * Low-level values of type `L` can be **decoded** to a higher-level value of type `H`. The decoding can fail;
 * this is represented by a result of type [[DecodeResult.Failure]]. Failures might occur due to format errors, wrong
 * arity, exceptions, or validation errors. Validators can be added through the `validate` com.fortysevendeg.thool.method.
 *
 * High-level values of type `H` can be **encoded** as a low-level value of type `L`.
 *
 * Mappings can be chained using one of the `map` functions.
 *
 * @param L The type of the low-level value.
 * @param H The type of the high-level value.
 */
public interface Mapping<L, H> {

  public fun rawDecode(l: L): DecodeResult<H>

  public fun encode(h: H): L

  /**
   * - calls `rawDecode`
   * - catches any exceptions that might occur, converting them to decode failures
   * - validates the result
   */
  public fun decode(l: L): DecodeResult<H> = tryRawDecode(l)

  private fun tryRawDecode(l: L): DecodeResult<H> =
    try {
      rawDecode(l)
    } catch (e: Throwable) {
      val error = e.nonFatalOrThrow()
      DecodeResult.Failure.Error(l.toString(), error)
    }

  public fun <HH> map(codec: Mapping<H, HH>): Mapping<L, HH> =
    object : Mapping<L, HH> {
      override fun rawDecode(l: L): DecodeResult<HH> =
        this@Mapping.rawDecode(l).flatMap(codec::rawDecode)

      override fun encode(h: HH): L =
        this@Mapping.encode(codec.encode(h))
    }

  public companion object {
    public fun <L> id(): Mapping<L, L> =
      object : Mapping<L, L> {
        override fun rawDecode(l: L): DecodeResult<L> = DecodeResult.Value(l)
        override fun encode(h: L): L = h
      }

    public fun <L, H> fromDecode(rawDecode: (L) -> DecodeResult<H>, encode: (H) -> L): Mapping<L, H> =
      object : Mapping<L, H> {
        override fun rawDecode(l: L): DecodeResult<H> = rawDecode(l)
        override fun encode(h: H): L = encode(h)
      }

    public fun <L, H> from(decode: (L) -> H, encode: (H) -> L): Mapping<L, H> =
      fromDecode(decode.andThen { DecodeResult.Value(it) }, encode)

    /**
     * A mapping which, during encoding, adds the given `prefix`.
     * When decoding, the prefix is removed (case insensitive,if present), otherwise an error is reported.
     */
    public fun stringPrefixCaseInsensitive(prefix: String): Mapping<String, String> {
      val prefixLength = prefix.length
      val prefixLower = prefix.lowercase()

      return fromDecode({ value ->
        if (value.lowercase().startsWith(prefixLower)) DecodeResult.Value(value.substring(prefixLength))
        else DecodeResult.Failure.Error(value, IllegalArgumentException("The given value doesn't start with $prefix"))
      }) { v -> "$prefix$v" }
    }
  }
}

public sealed class DecodeResult<out A> {
  public abstract fun <B> map(transform: (A) -> B): DecodeResult<B>
  public abstract fun <B> flatMap(transform: (A) -> DecodeResult<B>): DecodeResult<B>

  public data class Value<A>(val value: A) : DecodeResult<A>() {
    override fun <B> map(transform: (A) -> B): DecodeResult<B> = Value(transform(value))
    override fun <B> flatMap(transform: (A) -> DecodeResult<B>): DecodeResult<B> = transform(value)
  }

  public sealed class Failure : DecodeResult<Nothing>() {
    override fun <B> map(transform: (Nothing) -> B): DecodeResult<B> = this
    override fun <B> flatMap(transform: (Nothing) -> DecodeResult<B>): DecodeResult<B> = this

    public object Missing : Failure()
    public data class Multiple<A>(val values: List<A>) : Failure()
    public data class Mismatch(val expected: String, val actual: String) : Failure()
    public data class Error(val original: String, val error: Throwable) : Failure() {
      public companion object {
        public data class JsonDecodeException(val errors: List<JsonError>, val underlying: Throwable) : Exception(
          if (errors.isEmpty()) underlying.message else errors.joinToString(transform = JsonError::message),
          underlying
        )

        public data class JsonError(val msg: String, val path: List<FieldName>) {
          public fun message(): String {
            val at = if (path.isNotEmpty()) " at '${path.joinToString(separator = ".") { it.encodedName }}'" else ""
            return msg + at
          }
        }
      }
    }
  }

  public fun <A> fromOption(o: Option<A>): DecodeResult<A> =
    o.map { Value(it) }.getOrElse { Failure.Missing }
}

public fun <A> Iterable<DecodeResult<A>>.sequence(): DecodeResult<List<A>> = traverseDecodeResult(::identity)

public inline fun <A, B> Iterable<A>.traverseDecodeResult(f: (A) -> DecodeResult<B>): DecodeResult<List<B>> {
  val acc = mutableListOf<B>()
  forEach { a ->
    when (val res = f(a)) {
      is DecodeResult.Value -> acc.add(res.value)
      is DecodeResult.Failure -> return@traverseDecodeResult res
    }
  }
  return DecodeResult.Value(acc)
}
