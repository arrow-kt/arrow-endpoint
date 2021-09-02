package com.fortysevendeg.thool.ktor.server

import arrow.core.Either
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.ktor.client.invoke
import com.fortysevendeg.thool.ktor.client.responseToDomain
import com.fortysevendeg.thool.model.Body
import com.fortysevendeg.thool.model.ServerResponse
import com.fortysevendeg.thool.model.StatusCode
import com.fortysevendeg.thool.reduce
import com.fortysevendeg.thool.server.ServerEndpoint
import com.fortysevendeg.thool.test.CtxServerInterpreterSuite
import io.ktor.client.call.receive
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.readFully
import kotlinx.coroutines.runBlocking

class KtorServerInterpreterSuite : CtxServerInterpreterSuite<TestApplicationEngine>() {
  override suspend fun <A> withEndpoint(
    endpoint: ServerEndpoint<*, *, *>,
    run: suspend TestApplicationEngine.(baseString: String) -> A
  ): A = withTestApplication {
    application.install(endpoint)
    run("")
  }

  override suspend fun <I, E, O> TestApplicationEngine.requestAndResponse(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, ServerResponse> {
    val response = client.config { expectSuccess = false }.invoke(endpoint, baseUrl)(input)
    return Pair(
      endpoint.responseToDomain(response),
      ServerResponse(
        StatusCode(response.status.value),
        response.headers.toHeaders(),
        null // TODO("Extract the correct body type")
      ).also { println(it) }
    )
  }
}

inline fun <R> withApplication(
  environment: ApplicationEngineEnvironment = createTestEnvironment(),
  noinline configure: TestApplicationEngine.Configuration.() -> Unit = {},
  test: TestApplicationEngine.() -> R
): R {
  val engine = TestApplicationEngine(environment, configure)
  engine.start()
  try {
    return engine.test()
  } finally {
    engine.stop(0L, 0L)
  }
}

/**
 * Start test application engine, pass it to [test] function and stop it
 */
inline fun <R> withTestApplication(test: TestApplicationEngine.() -> R): R =
  withApplication(createTestEnvironment(), test = test)
