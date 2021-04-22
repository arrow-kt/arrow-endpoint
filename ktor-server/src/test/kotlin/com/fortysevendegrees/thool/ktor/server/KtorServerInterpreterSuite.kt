package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.server.ServerEndpoint
import com.fortysevendegrees.thool.test.ServerIntepreterSuite
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine

class KtorServerInterpreterSuite : ServerIntepreterSuite() {
  var server: NettyApplicationEngine? = null

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
  }

  override fun <I, E, O> install(endpoint: ServerEndpoint<I, E, O>) {
    server?.application?.install(endpoint)
  }
}
