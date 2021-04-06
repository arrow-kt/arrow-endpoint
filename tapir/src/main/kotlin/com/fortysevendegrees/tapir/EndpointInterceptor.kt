package com.fortysevendegrees.tapir

import com.fortysevendegrees.thool.model.Header
import com.fortysevendegrees.thool.model.ResponseMetadata
import com.fortysevendegrees.thool.model.StatusCode
import com.fortysevendegrees.thool.model.toStringSafe

/**
 * Allows intercepting the handling of a request by an endpoint, when either the endpoint's inputs have been
 * decoded successfully, or when decoding has failed.
 * @tparam B The interpreter-specific, low-level type of body.
 */
interface EndpointInterceptor<B> {

  /** Called when the the given `request` has been successfully decoded into inputs `i`, as described by
   * `endpoint.input`.
   *
   * Use `next(null)` to continue processing, ultimately (after the last interceptor) calling the endpoint's server
   * logic, and obtaining a response. Or, provide an alternative value+output pair, which will be used as the response.
   *
   * @tparam I The type of the endpoint's inputs.
   * @return An effect, describing the server's response.
   */
  suspend fun <I> onDecodeSuccess(
    request: ServerRequest,
    endpoint: Endpoint<I, *, *>,
    i: I,
    next: suspend (ValuedEndpointOutput<*>?) -> ServerResponse<B>
  ): ServerResponse<B> = next(null)

  /** Called when the the given `request` hasn't been successfully decoded into inputs `i`, as described by `endpoint`,
   * with `failure` occurring when decoding `failingInput`.
   *
   * Use `next(None)` to continue processing, ultimately (after the last interceptor) returning `None`, and attempting
   * to decode the next endpoint. Or, provide an alternative value+output pair, which will be used as the response.
   *
   * @return An effect, describing the optional server response. If `None`, the next endpoint will be tried (if any).
   */
  suspend fun onDecodeFailure(
    request: ServerRequest,
    endpoint: Endpoint<*, *, *>,
    failure: DecodeResult.Failure,
    failingInput: EndpointInput<*>,
    next: suspend (ValuedEndpointOutput<*>?) -> ServerResponse<B>?
  ): ServerResponse<B>? = next(null)
}

data class ValuedEndpointOutput<A>(val output: EndpointOutput<A>, val value: A)

data class ServerResponse<B>(override val code: StatusCode, override val headers: List<Header>, val body: B?) :
  ResponseMetadata {
  override val statusText: String = ""
  override fun toString(): String = "ServerResponse($code,${headers.toStringSafe()})"
}
