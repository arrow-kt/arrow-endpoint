package com.fortysevendegrees.thool.test

import arrow.core.Either
import arrow.core.right
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.model.QueryParams
import com.fortysevendegrees.thool.model.StatusCode
import com.fortysevendegrees.thool.test.TestEndpoint.delete_endpoint
import com.fortysevendegrees.thool.test.TestEndpoint.in_4query_out_4header_extended
import com.fortysevendegrees.thool.test.TestEndpoint.in_byte_array_out_byte_array
import com.fortysevendegrees.thool.test.TestEndpoint.in_byte_buffer_out_byte_buffer
import com.fortysevendegrees.thool.test.TestEndpoint.in_header_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_input_stream_out_input_stream
import com.fortysevendegrees.thool.test.TestEndpoint.in_json_out_json
import com.fortysevendegrees.thool.test.TestEndpoint.in_mapped_path_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_mapped_path_path_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_mapped_query_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_optional_json_out_optional_json
import com.fortysevendegrees.thool.test.TestEndpoint.in_path_path_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_paths_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_list_out_header_list
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_mapped_path_path_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_out_mapped_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_out_mapped_string_header
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_params_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_query_query_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_string_out_status
import com.fortysevendegrees.thool.test.TestEndpoint.in_string_out_string
import com.fortysevendegrees.thool.test.TestEndpoint.in_unit_out_json_unit
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockWebServer
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer

public abstract class ClientInterpreterSuite : FreeSpec() {
  private val server = MockWebServer()
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
      Triple(
        in_byte_array_out_byte_array.logic { it.right() },
        "banana kiwi".toByteArray(),
        Either.Right("banana kiwi".toByteArray())
      ),
      Triple(
        in_byte_buffer_out_byte_buffer.logic { it.right() },
        ByteBuffer.wrap("mango".toByteArray()),
        Either.Right(ByteBuffer.wrap("mango".toByteArray()))
      ),
      Triple(
        in_input_stream_out_input_stream.logic { it.right() },
        ByteArrayInputStream("mango".toByteArray()),
        Either.Right(ByteArrayInputStream("mango".toByteArray()))
      ),
      Triple(
        in_query_params_out_string.logic {
          it.ps.sortedBy(Pair<String, List<String>>::first)
            .joinToString("&") { p -> "${p.first}=${p.second.firstOrNull() ?: ""}" }.right()
        },
        QueryParams(mapOf("name" to "apple", "weight" to "42", "kind" to "very good")),
        Either.Right("kind=very good&name=apple&weight=42")
      ),
      Triple(
        in_paths_out_string.logic { it.joinToString(", ").right() },
        listOf("fruit", "apple", "amount", "50"),
        Either.Right("fruit, apple, amount, 50")
      ),
      Triple(
        in_query_list_out_header_list.logic { it.right() },
        listOf("plum", "watermelon", "apple"),
        Either.Right(listOf("plum", "watermelon", "apple"))
      ),
      Triple(in_string_out_status.logic { StatusCode.Ok.right() }, "apple", Either.Right(StatusCode.Ok)),
      Triple(delete_endpoint.logic { it.right() }, Unit, Either.Right(Unit)),
      Triple(
        in_optional_json_out_optional_json.name("defined").logic { it.right() },
        FruitAmount("orange", 11),
        Either.Right(FruitAmount("orange", 11))
      ),
      Triple(in_optional_json_out_optional_json.name("empty").logic { it.right() }, null, Either.Right(null)),
      Triple(
        in_4query_out_4header_extended.logic { it.right() },
        Triple(Pair("1", "2"), "3", "4"),
        Either.Right(Triple(Pair("1", "2"), "3", "4"))
      ),
      Triple(
        in_unit_out_json_unit.logic { it.right() },
        Unit,
        Either.Right(Unit)
      )
    ).forEach { (s, input, expected) ->
      s.endpoint.details().invoke {
        server.dispatcher = s.toDispatcher()

        fun adjust(r: Either<Any?, Any?>): Either<Any?, Any?> {
          fun doAdjust(v: Any?): Any? = when (v) {
            is InputStream -> v.readBytes().toList()
            is ByteArray -> v.toList()
            is ByteBuffer -> v.array().toList()
            is ByteArrayInputStream -> v.readBytes().toList()
            else -> v
          }

          return r.bimap(::doAdjust, ::doAdjust)
        }

        val result = request((s.endpoint as Endpoint<Any?, Any?, Any?>), baseUrl, input).map { adjust(it) }
        result shouldBe DecodeResult.Value(adjust(expected))
      }
    }
  }
}
