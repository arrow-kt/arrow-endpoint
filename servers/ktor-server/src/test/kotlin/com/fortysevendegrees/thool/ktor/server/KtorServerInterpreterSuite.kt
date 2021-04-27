package com.fortysevendegrees.thool.ktor.server

import arrow.core.Either
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.ktor.client.requestAndParse
import com.fortysevendegrees.thool.server.ServerEndpoint
import com.fortysevendegrees.thool.test.ServerIntepreterSuite
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.runBlocking

class KtorServerInterpreterSuite : ServerIntepreterSuite<TestApplicationEngine>() {
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
