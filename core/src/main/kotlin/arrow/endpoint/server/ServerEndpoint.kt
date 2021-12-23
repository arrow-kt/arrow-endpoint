package arrow.endpoint.server

import arrow.endpoint.Endpoint
import arrow.core.Either

/**
 * @param I Input parameter types.
 * @param E Error output parameter types.
 * @param O Output parameter types.
 * @param R The capabilities that are required by this endpoint's inputs/outputs. `Any`, if no requirements.
 */
public data class ServerEndpoint<I, E, O>(val endpoint: Endpoint<I, E, O>, val logic: suspend (I) -> Either<E, O>)
