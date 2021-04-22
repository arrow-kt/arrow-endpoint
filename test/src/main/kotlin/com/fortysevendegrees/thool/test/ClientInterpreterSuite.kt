package com.fortysevendegrees.thool.test

import arrow.core.Either
import arrow.core.right
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.output
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockWebServer

abstract class ClientInterpreterSuite : FreeSpec() {
//  var server: NettyApplicationEngine? = null
  val server = MockWebServer()
  var baseUrl: String = ""

  abstract suspend fun <I, E, O> request(endpoint: Endpoint<I, E, O>, input: I): DecodeResult<Either<E, O>>

  init {
//    beforeSpec {
//      val env = applicationEngineEnvironment {
//        connector {
//          host = "127.0.0.1"
//          port = 8080
//        }
//      }
//      server = embeddedServer(Netty, env).start(false)
//    }
//
//    afterSpec { server?.stop(1000, 1000) }
    beforeSpec {
      server.start()
      baseUrl = server.url("/").toString().also { println("#################################################### $it") }
    }
    afterSpec { server.close() }

    "test get" {
      val endpoint = Endpoint.get("ping").output(Thool.stringBody())
      server.dispatcher = endpoint.logic { "Pong".right() }.toDispatcher()
      request(endpoint, Unit) shouldBe DecodeResult.Value("Pong".right())
    }
  }
}
