package arrow.endpoint

import arrow.core.Either
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import io.kotest.matchers.shouldBe as coreShouldBe
import arrow.core.Either.Left
import arrow.core.Either.Right

// missing kotest-arrow targets for iosX64
internal infix fun <A> A.shouldBe(a: A): A {
  this coreShouldBe a
  return this
}

@OptIn(ExperimentalContracts::class)
public fun <A, B> Either<A, B>.shouldBeRight(failureMessage: (A) -> String = { "Expected Either.Right, but found Either.Left with value $it" }): B {
  contract {
    returns() implies (this@shouldBeRight is arrow.core.Either.Right<B>)
  }
  return when (this) {
    is Right -> value
    is Left -> throw AssertionError(failureMessage(value))
  }
}

public infix fun <A, B> Either<A, B>.shouldBeRight(b: B): B =
  shouldBeRight().shouldBe(b)

@OptIn(ExperimentalContracts::class)
public fun <A, B> Either<A, B>.shouldBeLeft(failureMessage: (B) -> String = { "Expected Either.Left, but found Either.Right with value $it" }): A {
  contract {
    returns() implies (this@shouldBeLeft is Left<A>)
  }
  return when (this) {
    is Left -> value
    is Right -> throw AssertionError(failureMessage(value))
  }
}

public infix fun <A, B> Either<A, B>.shouldBeLeft(a: A): A =
  shouldBeLeft().shouldBe(a)




