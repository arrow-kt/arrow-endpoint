package com.fortysevendeg.thool.test

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.Endpoint.Info
import com.fortysevendeg.thool.EndpointInput
import com.fortysevendeg.thool.EndpointOutput
import com.fortysevendeg.thool.Thool
import com.fortysevendeg.thool.http4k.client.execute
import com.fortysevendeg.thool.input
import com.fortysevendeg.thool.model.Method
import com.fortysevendeg.thool.model.StatusCode
import com.fortysevendeg.thool.server.ServerEndpoint
import com.fortysevendeg.thool.test.TestEndpoint.in_byte_array_out_byte_array
import com.fortysevendeg.thool.test.TestEndpoint.in_header_before_path
import com.fortysevendeg.thool.test.TestEndpoint.in_header_out_string
import com.fortysevendeg.thool.test.TestEndpoint.out_value_form_exact_match
import com.fortysevendeg.thool.test.TestEndpoint.in_json_out_json
import com.fortysevendeg.thool.test.TestEndpoint.in_mapped_path_out_string
import com.fortysevendeg.thool.test.TestEndpoint.in_mapped_path_path_out_string
import com.fortysevendeg.thool.test.TestEndpoint.in_mapped_query_out_string
import com.fortysevendeg.thool.test.TestEndpoint.in_path_path_out_string
import com.fortysevendeg.thool.test.TestEndpoint.in_query_mapped_path_path_out_string
import com.fortysevendeg.thool.test.TestEndpoint.in_query_out_infallible_string
import com.fortysevendeg.thool.test.TestEndpoint.in_query_out_mapped_string
import com.fortysevendeg.thool.test.TestEndpoint.in_query_out_mapped_string_header
import com.fortysevendeg.thool.test.TestEndpoint.in_query_out_string
import com.fortysevendeg.thool.test.TestEndpoint.in_query_query_out_string
import com.fortysevendeg.thool.test.TestEndpoint.out_status_from_string_one_empty
import com.fortysevendeg.thool.test.TestEndpoint.out_reified_status
import com.fortysevendeg.thool.test.TestEndpoint.in_string_out_string
import com.fortysevendeg.thool.test.TestEndpoint.in_two_path_capture
import com.fortysevendeg.thool.test.TestEndpoint.in_unit_out_json_unit
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.http4k.client.ApacheClient
import org.http4k.core.Request
import org.http4k.core.Response

/**
 * Overload for CtxServerInterpreterSuite where there is no Server Ctx needs to be threaded between server and client functions
 **/
public abstract class ServerInterpreterSuite : CtxServerInterpreterSuite<Unit>()

/**
 * Abstract server interpreter test suite
 *
 * Allows for [Ctx] to be threaded between [withEndpoint] and [request].
 * This is useful for testing frameworks that have specific testing support like Ktor,
 * with these testing frameworks we often have to pass along some test context to pass testing state
 * between server and client.
 *
 * See the Ktor module for an example that thread `TestApplicationEngine` between [withEndpoint] & [request].
 */
public abstract class CtxServerInterpreterSuite<Ctx> : FreeSpec() {

  private val client: (Request) -> Response = ApacheClient()

  public abstract suspend fun <A> withEndpoint(
    endpoint: ServerEndpoint<*, *, *>,
    run: suspend Ctx.(baseUrl: String) -> A
  ): A

  public open suspend fun <I, E, O> Ctx.requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (_, resp, result) = client.execute(endpoint, baseUrl, input)
    return Pair(result, StatusCode(resp.status.code))
  }

  private suspend fun <I, E, O> Ctx.request(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): DecodeResult<Either<E, O>> =
    requestAndStatusCode(endpoint, baseUrl, input).first

  init {
    val empty = Endpoint(EndpointInput.empty(), EndpointOutput.empty(), EndpointOutput.empty(), Info.empty())
    val emptyGet = empty.input(Thool.method(Method.GET))
    val emptyPost = empty.input(Thool.method(Method.POST))

    "Empty endpoint matches all methods" {
      withEndpoint(empty.logic { it.right() }) { baseUrl ->
        val (res, code) = requestAndStatusCode(emptyPost, baseUrl, Unit)
        res shouldBe DecodeResult.Value(Unit.right())
        code shouldBe StatusCode.Ok
      }
    }

    "Mismatching endpoint results in NotFound" {
      withEndpoint(emptyGet.logic { it.right() }) { baseUrl ->
        val (res, code) = requestAndStatusCode(emptyPost, baseUrl, Unit)
        res shouldBe DecodeResult.Value(Unit.left())
        code shouldBe StatusCode.NotFound
      }
    }

    fun <I, E, O> test(
      endpoint: Endpoint<I, E, O>,
      input: I,
      expected: Either<E, O>,
      postfix: String = "",
      logic: suspend (input: I) -> Either<E, O>
    ): Unit =
      "${endpoint.details()} - $postfix" {
        withEndpoint(endpoint.logic(logic)) { baseUrl ->
          request(endpoint, baseUrl, input).map(::normalise) shouldBe DecodeResult.Value(normalise(expected))
        }
      }

    fun <E, O> test(
      endpoint: Endpoint<Unit, E, O>,
      expected: Either<E, O>,
      postfix: String = "",
      logic: suspend (input: Unit) -> Either<E, O>
    ): Unit =
      "${endpoint.details()} - $postfix" {
        withEndpoint(endpoint.logic(logic)) { baseUrl ->
          request(endpoint, baseUrl, Unit).map(::normalise) shouldBe DecodeResult.Value(normalise(expected))
        }
      }

    fun <I, E, O> test(
      endpoint: Endpoint<I, E, O>,
      input: I,
      expected: Pair<Either<E, O>, StatusCode>,
      postfix: String = "",
      logic: suspend (input: I) -> Either<E, O>
    ): Unit =
      "${endpoint.details()} - $postfix" {
        withEndpoint(endpoint.logic(logic)) { baseUrl ->
          val (normalised, code) = requestAndStatusCode(endpoint, baseUrl, input)
          Pair(normalised, code) shouldBe Pair(DecodeResult.Value(normalise(expected.first)), expected.second)
        }
      }

    fun <E, O> test(
      endpoint: Endpoint<Unit, E, O>,
      expected: Pair<Either<E, O>, StatusCode>,
      postfix: String = "",
      logic: suspend (input: Unit) -> Either<E, O>
    ): Unit =
      "${endpoint.details()} - $postfix" {
        withEndpoint(endpoint.logic(logic)) { baseUrl ->
          val (normalised, code) = requestAndStatusCode(endpoint, baseUrl, Unit)
          Pair(normalised, code) shouldBe Pair(DecodeResult.Value(normalise(expected.first)), expected.second)
        }
      }

    test(Endpoint.get(), Unit, Unit.right(), "GET a GET endpoint") { it.right() }
    test(in_query_out_string, "orange", "orange".right()) { it.right() }
    test(in_query_out_string, "red apple", "red apple".right(), "URL encoding") { it.right() }
    test(empty, Unit, Unit.right(), "GET empty endpoint") { it.right() }

    test(
      in_query_out_infallible_string,
      "kiwi",
      "fruit: kiwi".right()
    ) { "fruit: $it".right() }

    test(
      in_query_query_out_string,
      Pair("orange", 10),
      "orange: 10".right(),
      "Defined"
    ) { (f, a) -> "$f: $a".right() }

    test(
      in_query_query_out_string,
      Pair("orange", null),
      "orange: null".right(),
      "Null"
    ) { (f, a) -> "$f: $a".right() }

    test(
      in_header_out_string,
      "Admin",
      "Admin".right()
    ) { it.right() }

    test(
      in_path_path_out_string,
      Pair("orange", 20),
      "orange: 20".right()
    ) { (fruit, amount) -> "$fruit: $amount".right() }

//  TODO fix URL encoding => expected: "apple/red: 20" but was: "apple%2Fred: 20"
//  this one works for the Spring interpreter
//    test(
//      in_path_path_out_string,
//      Pair("apple/red", 20),
//      "apple/red: 20".right(),
//      "URL encoding"
//    ) { (fruit, amount) -> "$fruit: $amount".right() }

    test(
      in_two_path_capture,
      Pair(12, 23),
      Pair(12, 23).right(),
      "capturing two path parameters with the same specification"
    ) { it.right() }

    test(
      in_string_out_string,
      "Sweet",
      "Sweet".right()
    ) { it.right() }

    test(
      in_mapped_query_out_string,
      "orange".toList(),
      "length: 6".right()
    ) { l -> "length: ${l.size}".right() }

    test(
      in_mapped_path_out_string,
      Fruit("kiwi"),
      "kiwi".right()
    ) { it.name.right() }

    test(
      in_mapped_path_path_out_string,
      FruitAmount("orange", 10),
      "FA: FruitAmount(orange, 10)".right()
    ) { (f, a) -> "FA: FruitAmount($f, $a)".right() }

    test(
      in_query_mapped_path_path_out_string,
      Pair(FruitAmount("orange", 10), "yellow"),
      "FA: FruitAmount(orange, 10) color: yellow".right()
    ) { (fa, color) -> "FA: FruitAmount(${fa.fruit}, ${fa.amount}) color: $color".right() }

    test(
      in_query_out_mapped_string,
      "orange",
      "orange".toList().right()
    ) { it.toList().right() }

    test(
      in_query_out_mapped_string_header,
      "orange",
      FruitAmount("orange", 6).right()
    ) { FruitAmount(it, it.length).right() }

    test(
      in_header_before_path,
      Pair("hello", 12),
      Pair(12, "hello").right(),
      "Header input before path capture input"
    ) { it.reversed().right() }

    test(
      in_json_out_json,
      FruitAmount("orange", 11),
      FruitAmount("orange banana", 22).right()
    ) { (fruit, amount) -> FruitAmount("$fruit banana", amount * 2).right() }

    test(
      in_byte_array_out_byte_array,
      "banana kiwi".toByteArray(),
      "banana kiwi".toByteArray().right(),
    ) { it.right() }

    test(
      in_unit_out_json_unit,
      Unit.right(),
      "unit json mapper"
    ) { it.right() }

    test(
      out_reified_status.name("status 1/2"),
      Pair("x".right().right(), StatusCode.Ok),
    ) { "x".right().right() }

    test(
      out_reified_status.name("status 2/2"),
      Pair(1.left().right(), StatusCode.Accepted),
    ) { 1.left().right() }

    test(
      out_value_form_exact_match.name("status A"),
      Pair("A".right(), StatusCode.Ok)
    ) { "A".right() }

    test(
      out_value_form_exact_match.name("status B"),
      Pair("B".right(), StatusCode.Accepted)
    ) { "B".right() }

    test(
      out_status_from_string_one_empty.name("status string"),
      Pair("x".right().right(), StatusCode.Ok)
    ) { "x".right().right() }

    test(
      out_status_from_string_one_empty.name("status empty"),
      Pair(Unit.left().right(), StatusCode.Accepted)
    ) { Unit.left().right() }
  }
}
