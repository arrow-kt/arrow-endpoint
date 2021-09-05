package arrow.endpoint.http4k

import arrow.core.Either
import arrow.endpoint.DecodeResult
import arrow.endpoint.Endpoint
import arrow.endpoint.http4k.client.execute
import arrow.endpoint.model.StatusCode
import arrow.endpoint.test.ClientInterpreterSuite
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler

class Http4KClientSuite : ClientInterpreterSuite() {

  private val client: HttpHandler = ApacheClient()

  override suspend fun <I, E, O> requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (_, resp, res) = client.execute(endpoint, baseUrl, input)
    return Pair(res, StatusCode(resp.status.code))
  }
}
