package com.fortysevendeg.thool.ktor.server

import arrow.core.Either
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.ktor.client.requestAndParse
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

  override suspend fun <I, E, O> TestApplicationEngine.request(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): DecodeResult<Either<E, O>> {
    val f = endpoint.requestAndParse(baseUrl)
    return client.config { expectSuccess = false }.f(input)
  }
}
