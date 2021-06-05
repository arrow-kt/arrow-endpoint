package com.fortysevendeg.thool.ktor.server

import arrow.core.Either
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.ktor.client.execute
import com.fortysevendeg.thool.model.StatusCode
import com.fortysevendeg.thool.server.ServerEndpoint
import com.fortysevendeg.thool.test.CtxServerInterpreterSuite
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
