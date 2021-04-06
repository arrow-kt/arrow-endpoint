package com.fortysevendegrees.tapir.server

import com.fortysevendegrees.tapir.Endpoint
import arrow.core.Either

/** @tparam I Input parameter types.
 * @tparam E Error output parameter types.
 * @tparam O Output parameter types.
 * @tparam R The capabilities that are required by this endpoint's inputs/outputs. `Any`, if no requirements.
 * @tparam F The effect type used in the provided server logic.
 */
data class ServerEndpoint<I, E, O>(val endpoint: Endpoint<I, E, O>, val logic: suspend (I) -> Either<E, O>)
