package com.fortysevendegrees.thool

import arrow.core.right
import com.fortysevendegrees.thool.ktor.server.install
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status

class ServerInterpreterIntegrationTest : FreeSpec({
  var server: NettyApplicationEngine? = null
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

  val client = ApacheClient()

  "endpoint can work with all methods" - {
    "get" {
      val endpoint = Endpoint.get().output(Thool.stringBody())
      val request = Request(Method.GET, "http://localhost:8080")
      val result = "TEST-GET"
      server?.application?.install(endpoint.logic { result.right() })
      client(request).bodyString() shouldBe result
    }
    "post" {
      val endpoint = Endpoint.post().output(Thool.stringBody())
      val request = Request(Method.POST, "http://localhost:8080")
      val result = "TEST-POST"
      server?.application?.install(endpoint.logic { result.right() })
      client(request).bodyString() shouldBe result
    }
  }
  "endpoint can output" - {
    "stringBody" {
      val endpoint = Endpoint.get("stringBody").output(Thool.stringBody())
      val request = Request(Method.GET, "http://localhost:8080/stringBody")
      val result = "stringBody"
      server?.application?.install(endpoint.logic { result.right() })

      assertSoftly(client(request)) {
        status shouldBe Status.OK
        bodyString() shouldBe result
      }
    }
    "jsonBody" {
      val endpoint = Endpoint.get("jsonBody").output(Thool.anyJsonBody(Codec.person()))
      val request = Request(Method.GET, "http://localhost:8080/jsonBody")
      val result = """{"name":"John", "age":31}"""

      server?.application?.install(endpoint.logic { Json.decodeFromString<Person>(result).right() })

      assertSoftly(client(request)) {
        status shouldBe Status.OK
        bodyString() shouldBe result
      }
    }
  }
})
