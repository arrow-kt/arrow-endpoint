package arrow.endpoint.test

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.endpoint.DecodeResult
import arrow.endpoint.Endpoint
import arrow.endpoint.EndpointInput
import arrow.endpoint.model.QueryParams
import arrow.endpoint.model.StatusCode
import arrow.endpoint.test.TestEndpoint.delete_endpoint
import arrow.endpoint.test.TestEndpoint.in_4query_out_4header_extended
import arrow.endpoint.test.TestEndpoint.in_byte_array_out_byte_array
import arrow.endpoint.test.TestEndpoint.in_header_out_string
import arrow.endpoint.test.TestEndpoint.out_value_form_exact_match
import arrow.endpoint.test.TestEndpoint.in_json_out_json
import arrow.endpoint.test.TestEndpoint.in_mapped_path_out_string
import arrow.endpoint.test.TestEndpoint.in_mapped_path_path_out_string
import arrow.endpoint.test.TestEndpoint.in_mapped_query_out_string
import arrow.endpoint.test.TestEndpoint.in_optional_json_out_optional_json
import arrow.endpoint.test.TestEndpoint.in_path_path_out_string
import arrow.endpoint.test.TestEndpoint.in_paths_out_string
import arrow.endpoint.test.TestEndpoint.in_query_list_out_header_list
import arrow.endpoint.test.TestEndpoint.in_query_mapped_path_path_out_string
import arrow.endpoint.test.TestEndpoint.in_query_out_mapped_string
import arrow.endpoint.test.TestEndpoint.in_query_out_mapped_string_header
import arrow.endpoint.test.TestEndpoint.in_query_out_string
import arrow.endpoint.test.TestEndpoint.in_query_params_out_string
import arrow.endpoint.test.TestEndpoint.in_query_query_out_string
import arrow.endpoint.test.TestEndpoint.in_string_out_status
import arrow.endpoint.test.TestEndpoint.out_reified_status
import arrow.endpoint.test.TestEndpoint.in_string_out_string
import arrow.endpoint.test.TestEndpoint.in_unit_out_json_unit
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockWebServer
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

public abstract class ClientInterpreterSuite : FreeSpec() {
  private val server = MockWebServer()
  private var baseUrl: String = ""

  public abstract suspend fun <I, E, O> requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode>

  public suspend fun <I, E, O> request(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): DecodeResult<Either<E, O>> =
    requestAndStatusCode(endpoint, baseUrl, input).first

  init {
    beforeSpec {
      @Suppress("BlockingMethodInNonBlockingContext")
      withContext(Dispatchers.IO) { server.start() }
      baseUrl = server.url("/").toString()
    }
    @Suppress("BlockingMethodInNonBlockingContext")
    afterSpec { withContext(Dispatchers.IO) { server.close() } }

    fun <I, E, O> test(
      endpoint: Endpoint<I, E, O>,
      input: I,
      expected: Either<E, O>,
      logic: suspend (input: I) -> Either<E, O>
    ): Unit = endpoint.details().invoke {
      server.dispatcher = endpoint.logic(logic).toDispatcher()
      val result = request(endpoint, baseUrl, input).map(::normalise)
      result shouldBe DecodeResult.Value(normalise(expected))
    }

    fun <E, O> test(
      endpoint: Endpoint<Unit, E, O>,
      expected: Either<E, O>,
      logic: suspend (input: Unit) -> Either<E, O>
    ): Unit = test(endpoint, Unit, expected, logic)

    fun <I, E, O> test(
      endpoint: Endpoint<I, E, O>,
      input: I,
      expected: Pair<Either<E, O>, StatusCode>,
      logic: suspend (input: I) -> Either<E, O>
    ): Unit = endpoint.details().invoke {
      server.dispatcher = endpoint.logic(logic).toDispatcher()
      val (res, code) = requestAndStatusCode(endpoint, baseUrl, input)
      Pair(res.map(::normalise), code) shouldBe Pair(DecodeResult.Value(normalise(expected.first)), expected.second)
    }

    fun <E, O> test(
      endpoint: Endpoint<Unit, E, O>,
      expected: Pair<Either<E, O>, StatusCode>,
      logic: suspend (input: Unit) -> Either<E, O>
    ): Unit = test(endpoint, Unit, expected, logic)

    test(Endpoint.input(EndpointInput.empty()), Unit, Either.Right(Unit)) { it.right() }
    test(in_query_out_string, "apple", Either.Right("apple")) { it.right() }
    test(in_query_query_out_string, Pair("apple", 10), Either.Right("(apple, 10)")) { it.toString().right() }
    test(in_header_out_string, "Admin", Either.Right("Admin")) { it.right() }
    test(in_path_path_out_string, Pair("apple", 10), Either.Right("(apple, 10)")) { it.toString().right() }
    test(in_string_out_string, "delicious", Either.Right("delicious")) { it.right() }
    test(in_mapped_query_out_string, "apple".toList(), Either.Right("apple")) { it.joinToString("").right() }
    test(in_mapped_path_out_string, Fruit("kiwi"), Either.Right("kiwi")) { it.name.right() }

    test(
      in_mapped_path_path_out_string,
      FruitAmount("apple", 10),
      Either.Right("(apple, 10)")
    ) { (n, i) -> "($n, $i)".right() }

    test(
      in_query_mapped_path_path_out_string,
      Pair(FruitAmount("apple", 10), "red"),
      Either.Right("(apple, 10, red)")
    ) { (fruitAmount, color) -> "(${fruitAmount.fruit}, ${fruitAmount.amount}, $color)".right() }

    test(in_query_out_mapped_string, "apple", Either.Right("apple".toList())) { it.toList().right() }

    test(in_query_out_mapped_string_header, "apple", Either.Right(FruitAmount("apple", 5))) {
      FruitAmount(
        it,
        5
      ).right()
    }

    test(in_json_out_json, FruitAmount("orange", 11), Either.Right(FruitAmount("orange", 11))) { it.right() }

    test(
      in_byte_array_out_byte_array,
      "banana kiwi".toByteArray(),
      Either.Right("banana kiwi".toByteArray())
    ) { it.right() }

    test(
      in_query_params_out_string,
      QueryParams(mapOf("name" to "apple", "weight" to "42", "kind" to "very good")),
      Either.Right("kind=very good&name=apple&weight=42")
    ) {
      it.all().sortedBy(Pair<String, List<String>>::first)
        .joinToString("&") { p -> "${p.first}=${p.second.firstOrNull() ?: ""}" }.right()
    }

    test(
      in_paths_out_string,
      listOf("fruit", "apple", "amount", "50"),
      Either.Right("fruit, apple, amount, 50")
    ) { it.joinToString(", ").right() }

    test(
      in_query_list_out_header_list,
      listOf("plum", "watermelon", "apple"),
      Either.Right(listOf("plum", "watermelon", "apple"))
    ) { it.right() }

    test(in_string_out_status, "apple", Either.Right(StatusCode.Ok)) { StatusCode.Ok.right() }

    test(delete_endpoint, Unit, Either.Right(Unit)) { it.right() }

    test(
      in_optional_json_out_optional_json.name("defined"),
      FruitAmount("orange", 11),
      Either.Right(FruitAmount("orange", 11))
    ) { it.right() }

    test(in_optional_json_out_optional_json.name("empty"), null, Either.Right(null)) { it.right() }

    test(
      in_4query_out_4header_extended,
      Triple(Pair("1", "2"), "3", "4"),
      Either.Right(Triple(Pair("1", "2"), "3", "4"))
    ) { it.right() }

    test(in_unit_out_json_unit, Unit, Either.Right(Unit)) { it.right() }

    test(
      out_reified_status.name("status 1/2"),
      Pair("fruit: apple".right().right(), StatusCode.Ok)
    ) { "fruit: apple".right().right() }

    test(
      out_reified_status.name("status 2/2"),
      Pair(29.left().right(), StatusCode.Accepted)
    ) { 29.left().right() }

    test(
      out_value_form_exact_match.name("first exact status of 2"),
      Pair("B".right(), StatusCode.Accepted)
    ) { "B".right() }

    test(
      out_value_form_exact_match.name("second exact status of 2"),
      Pair("A".right(), StatusCode.Ok)
    ) { "A".right() }
  }
}
