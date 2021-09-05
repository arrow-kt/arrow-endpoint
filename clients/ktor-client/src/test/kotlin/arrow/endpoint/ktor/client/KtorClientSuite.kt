package arrow.endpoint.ktor.client

import arrow.core.Either
import arrow.endpoint.DecodeResult
import arrow.endpoint.Endpoint
import arrow.endpoint.model.StatusCode
import arrow.endpoint.test.ClientInterpreterSuite
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

class KtorClientSuite : ClientInterpreterSuite() {

  private val client = HttpClient(CIO)

  override suspend fun <I, E, O> requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (_, response, result) = client.config { expectSuccess = false }.execute(endpoint, baseUrl, input)
    return Pair(result, StatusCode(response.status.value))
  }
}
