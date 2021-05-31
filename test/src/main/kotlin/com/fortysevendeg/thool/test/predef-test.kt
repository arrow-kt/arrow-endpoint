package com.fortysevendeg.thool.test

import arrow.core.Either
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer

/** Normalises the body to be correctly compared by Any::equals **/
internal fun normalise(either: Either<Any?, Any?>): Either<Any?, Any?> {
  fun doAdjust(v: Any?): Any? = when (v) {
    is InputStream -> v.readBytes().toList()
    is ByteArray -> v.toList()
    is ByteBuffer -> v.array().toList()
    is ByteArrayInputStream -> v.readBytes().toList()
    else -> v
  }

  return either.bimap(::doAdjust, ::doAdjust)
}

internal fun <A, B> Pair<A, B>.reversed(): Pair<B, A> = Pair(second, first)
