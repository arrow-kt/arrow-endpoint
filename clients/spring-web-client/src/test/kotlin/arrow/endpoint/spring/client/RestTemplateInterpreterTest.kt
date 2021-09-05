package arrow.endpoint.spring.client

import arrow.core.Either
import arrow.endpoint.DecodeResult
import arrow.endpoint.Endpoint
import arrow.endpoint.model.StatusCode
import arrow.endpoint.test.ClientInterpreterSuite
import org.springframework.web.client.RestTemplate

class RestTemplateInterpreterTest : ClientInterpreterSuite() {

  private val client = RestTemplate()

  override suspend fun <I, E, O> requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (_, response, result) = client.execute(endpoint, baseUrl, input)
    return Pair(result, StatusCode(response.rawStatusCode))
  }
}
