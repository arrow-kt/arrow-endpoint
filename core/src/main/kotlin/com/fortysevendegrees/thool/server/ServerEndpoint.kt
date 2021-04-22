package com.fortysevendegrees.thool.server

import com.fortysevendegrees.thool.Endpoint
import arrow.core.Either

/** @tparam I Input parameter types.
 * @tparam E Error output parameter types.
 * @tparam O Output parameter types.
 * @tparam R The capabilities that are required by this endpoint's inputs/outputs. `Any`, if no requirements.
 */
public data class ServerEndpoint<I, E, O>(val endpoint: Endpoint<I, E, O>, val logic: suspend (I) -> Either<E, O>)
