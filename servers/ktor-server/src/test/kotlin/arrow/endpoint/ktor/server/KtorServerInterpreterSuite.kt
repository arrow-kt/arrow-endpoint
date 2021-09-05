package arrow.endpoint.ktor.server

import arrow.core.Either
import arrow.endpoint.DecodeResult
import arrow.endpoint.Endpoint
import arrow.endpoint.ktor.client.execute
import arrow.endpoint.model.StatusCode
import arrow.endpoint.server.ServerEndpoint
import arrow.endpoint.test.CtxServerInterpreterSuite
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.runBlocking

class KtorServerInterpreterSuite : CtxServerInterpreterSuite<TestApplicationEngine>() {
  override suspend fun <A> withEndpoint(
    endpoint: ServerEndpoint<*, *, *>,
    run: suspend TestApplicationEngine.(baseString: String) -> A
  ): A = withTestApplication {
    application.install(endpoint)
    runBlocking { run("") }
  }

  override suspend fun <I, E, O> TestApplicationEngine.requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (_, response, result) = client.config { expectSuccess = false }.execute(endpoint, baseUrl, input)
    return Pair(result, StatusCode(response.status.value))
  }
}
