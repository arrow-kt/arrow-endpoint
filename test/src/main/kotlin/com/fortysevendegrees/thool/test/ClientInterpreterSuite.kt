package com.fortysevendegrees.thool.test

import arrow.core.right
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.ktor.server.install
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.output
import invoke
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import org.http4k.client.ApacheClient

class ClientInterpreterSuite : FreeSpec() {
  var server: NettyApplicationEngine? = null

  val client = ApacheClient()

  // abstract fun <I, E, O> request(endpoint: Endpoint<I, E, O>, input: I): DecodeResult<Either<E, O>>

  init {
    beforeSpec {
      val env = applicationEngineEnvironment {
        connector {
          host = "127.0.0.1"
          port = 8080
        }
      }
      server = embeddedServer(Netty, env).start(false)
    }

    afterSpec { server?.stop(1000, 1000) }

    "test get" {
      val endpoint = Endpoint.get("ping").output(Thool.stringBody())
      server?.application?.install(endpoint.logic { "Pong".right() })
      client.invoke(endpoint, "http://localhost:8080", Unit) shouldBe DecodeResult.Value("Pong".right())
    }
  }
}
