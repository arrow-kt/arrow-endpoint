package com.fortysevendegrees.thool.test

import arrow.core.Either
import arrow.core.right
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.Thool.anyJsonBody
import com.fortysevendegrees.thool.Thool.stringBody
import com.fortysevendegrees.thool.output
import com.fortysevendegrees.thool.test.TestEndpoint.in_header_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_json_out_json
import com.fortysevendegrees.thool.test.TestEndpoint.in_mapped_path_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_mapped_path_path_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_mapped_query_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_path_path_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_mapped_path_path_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_out_mapped_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_out_mapped_string_header
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_query_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_string_out_string
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

    listOf(
      Triple(Endpoint.input(EndpointInput.empty()).logic { it.right() }, Unit, Either.Right(Unit)),
      Triple(in_query_out_string.logic { it.right() }, "apple", Either.Right("apple")),
      Triple(in_query_query_out_string.logic { it.toString().right() }, Pair("apple", 10), Either.Right("(apple, 10)")),
      Triple(in_header_out_string.logic { it.right() }, "Admin", Either.Right("Admin")),
      Triple(in_path_path_out_string.logic { it.toString().right() }, Pair("apple", 10), Either.Right("(apple, 10)")),
      Triple(in_string_out_string.logic { it.right() }, "delicious", Either.Right("delicious")),
      Triple(in_mapped_query_out_string.logic { it.joinToString("").right() }, "apple".toList(), Either.Right("apple")),
      Triple(in_mapped_path_out_string.logic { it.name.right() }, Fruit("kiwi"), Either.Right("kiwi")),
      Triple(
        in_mapped_path_path_out_string.logic { (n, i) -> "($n, $i)".right() },
        FruitAmount("apple", 10),
        Either.Right("(apple, 10)")
      ),
      Triple(
        in_query_mapped_path_path_out_string.logic { (fruitAmount, color) -> "(${fruitAmount.fruit}, ${fruitAmount.amount}, $color)".right() },
        Pair(FruitAmount("apple", 10), "red"),
        Either.Right("(apple, 10, red)")
      ),
      Triple(in_query_out_mapped_string.logic { it.toList().right() }, "apple", Either.Right("apple".toList())),
      Triple(
        in_query_out_mapped_string_header.logic { FruitAmount(it, 5).right() },
        "apple",
        Either.Right(FruitAmount("apple", 5))
      ),
      Triple(in_json_out_json.logic { it.right() }, FruitAmount("orange", 11), Either.Right(FruitAmount("orange", 11))),
//    Triple(in_byte_array_out_byte_array, "banana kiwi".getBytes(), Either.Right("banana kiwi".getBytes())),
//    Triple(in_byte_buffer_out_byte_buffer, ByteBuffer.wrap("mango".getBytes), Either.Right(ByteBuffer.wrap("mango".getBytes)))
    ).forEach { (s, input, expected) ->
      s.endpoint.details().invoke {
        server.dispatcher = s.toDispatcher()
        request((s.endpoint as Endpoint<Any?, Any?, Any?>), baseUrl, input) shouldBe DecodeResult.Value(expected)
      }
    }

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
