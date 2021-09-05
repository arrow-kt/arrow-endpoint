package arrow.endpoint.spring.client

import arrow.core.Either
import arrow.endpoint.DecodeResult
import arrow.endpoint.Endpoint
import arrow.endpoint.model.StatusCode
import arrow.endpoint.test.ClientInterpreterSuite
import org.springframework.web.reactive.function.client.WebClient

class WebClientInterpreterTest : ClientInterpreterSuite() {

  private val client = WebClient.create()

  override suspend fun <I, E, O> requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (_, resp, res) = client.execute(endpoint, baseUrl, input)
    return Pair(res, StatusCode(resp.rawStatusCode()))
  }
}
