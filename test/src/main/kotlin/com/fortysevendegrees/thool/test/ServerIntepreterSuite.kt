package com.fortysevendegrees.thool.test

import arrow.core.right
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool.anyJsonBody
import com.fortysevendegrees.thool.Thool.stringBody
import com.fortysevendegrees.thool.output
import com.fortysevendegrees.thool.server.ServerEndpoint
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status

abstract class ServerIntepreterSuite : FreeSpec() {
  abstract fun <I, E, O> install(endpoint: ServerEndpoint<I, E, O>): Unit

  init {
    val client = ApacheClient()

    "endpoint can work with all methods" - {
      "get" {
        val endpoint = Endpoint.get().output(stringBody())
        val request = Request(Method.GET, "http://localhost:8080")
        val result = "TEST-GET"
        install(endpoint.logic { result.right() })

        client(request).bodyString() shouldBe result
      }
      "post" {
        val endpoint = Endpoint.post().output(stringBody())
        val request = Request(Method.POST, "http://localhost:8080")
        val result = "TEST-POST"
        install(endpoint.logic { result.right() })

        client(request).bodyString() shouldBe result
      }
    }
    "endpoint can output" - {
      "stringBody" {
        val endpoint = Endpoint.get("stringBody").output(stringBody())
        val request = Request(Method.GET, "http://localhost:8080/stringBody")
        val result = "stringBody"
        install(endpoint.logic { result.right() })

        assertSoftly(client(request)) {
          status shouldBe Status.OK
          bodyString() shouldBe result
        }
      }
      "jsonBody" {
        val endpoint = Endpoint.get("jsonBody").output(anyJsonBody(Codec.person()))
        val request = Request(Method.GET, "http://localhost:8080/jsonBody")
        val result = Person("John", 31)

        install(endpoint.logic { result.right() })

        assertSoftly(client(request)) {
          status shouldBe Status.OK
          bodyString() shouldBe Json.encodeToString(result)
        }
      }
    }
  }
}
