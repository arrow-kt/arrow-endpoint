package com.fortysevendegrees.thool.test

import arrow.core.Either
import arrow.core.right
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool.anyJsonBody
import com.fortysevendegrees.thool.Thool.stringBody
import com.fortysevendegrees.thool.output
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockWebServer

public abstract class ClientInterpreterSuite : FreeSpec() {
  public val server = MockWebServer()
  private var baseUrl: String = ""

  public abstract suspend fun <I, E, O> request(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): DecodeResult<Either<E, O>>

  init {
    beforeSpec {
      server.start()
      baseUrl = server.url("/").toString()
    }
    afterSpec { server.close() }

    "methods" - {
      "get" {
        val endpoint = Endpoint.get("ping").output(stringBody())
        server.dispatcher = endpoint.logic { "Pong".right() }.toDispatcher()
        request(endpoint, baseUrl, Unit) shouldBe DecodeResult.Value("Pong".right())
      }
      "put" {
        val endpoint = Endpoint.put("ping2").output(stringBody())
        server.dispatcher = endpoint.logic { "Pong".right() }.toDispatcher()
        request(endpoint, baseUrl, Unit) shouldBe DecodeResult.Value("Pong".right())
      }
    }
    "output" - {
      "stringBody" {
        val endpoint = Endpoint.get("stringBody").output(stringBody())
        val result = "stringBody".right()
        server.dispatcher = endpoint.logic { result }.toDispatcher()
        request(endpoint, baseUrl, Unit) shouldBe DecodeResult.Value(result)
      }
      "jsonBody" {
        val endpoint = Endpoint.get("jsonBody").output(anyJsonBody(Codec.person()))
        val result = Person("John", 31)
        server.dispatcher = endpoint.logic { result.right() }.toDispatcher()
        request(endpoint, baseUrl, Unit) shouldBe DecodeResult.Value(result.right())
      }
    }
  }
}
