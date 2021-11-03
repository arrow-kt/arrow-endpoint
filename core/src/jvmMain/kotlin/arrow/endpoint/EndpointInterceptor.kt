package arrow.endpoint

import arrow.endpoint.model.ServerRequest
import arrow.endpoint.model.ServerResponse

/**
 * Allows intercepting the handling of a request by an endpoint, when either the endpoint's inputs
 * have been decoded successfully, or when decoding has failed.
 */
public interface EndpointInterceptor {

  /**
   * Called when the the given [request] has been successfully decoded into inputs [I], as described
   * by [endpoint].
   *
   * Use `next(null)` to continue processing, ultimately (after the last interceptor) calling the
   * endpoint's server logic, and obtaining a response. Or, provide an alternative value+output
   * pair, which will be used as the response.
   *
   * @param I The type of the endpoint's inputs.
   * @return An effect, describing the server's response.
   */
  public suspend fun <I> onDecodeSuccess(
    request: ServerRequest,
    endpoint: Endpoint<I, *, *>,
    i: I,
    next: suspend (ValuedEndpointOutput<*>?) -> ServerResponse
  ): ServerResponse = next(null)

  /**
   * Called when the the given [request] hasn't been successfully decoded into inputs, as described
   * by [endpoint], with [failure] occurring when decoding [failingInput].
   *
   * Use `next(null)` to continue processing, ultimately (after the last interceptor) returning
   * `null`, and attempting to decode the next endpoint. Or, provide an alternative value+output
   * pair, which will be used as the response.
   *
   * @return An effect, describing the optional server response. If `null`, the next endpoint will
   * be tried (if any).
   */
  public suspend fun onDecodeFailure(
    request: ServerRequest,
    endpoint: Endpoint<*, *, *>,
    failure: DecodeResult.Failure,
    failingInput: EndpointInput<*>,
    next: suspend (ValuedEndpointOutput<*>?) -> ServerResponse?
  ): ServerResponse? = next(null)
}

public data class ValuedEndpointOutput<A>(val output: EndpointOutput<A>, val value: A)
