package com.fortysevendegrees.thool.test

import arrow.core.Either
import arrow.core.Tuple4
import arrow.core.left
import arrow.core.right
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointInfo
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.EndpointOutput
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.http4k.invoke
import com.fortysevendegrees.thool.input
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.server.ServerEndpoint
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.http4k.client.ApacheClient
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer

// This test suite is order sensitive.
// Edge case routes are installed with "/" without methods, these are order sensitive and need to run first.
// To solve or leviate that problem we should launch a new server for every test.
public abstract class ServerIntepreterSuite : FreeSpec() {
  public abstract fun <I, E, O> install(endpoint: ServerEndpoint<I, E, O>): Unit

  init {
    val empty = Endpoint(EndpointInput.empty(), EndpointOutput.empty(), EndpointOutput.empty(), EndpointInfo.empty())
    val get = empty.input(Thool.method(Method.GET))
    val post = empty.input(Thool.method(Method.POST))
    val client = ApacheClient()

//    "POST a GET endpoint" {
//      install(get.logic { it.right() })
//      client.invoke(post, "http://localhost:8080", Unit) shouldBe DecodeResult.Value(Unit.left())
//    }

//    "POST empty endpoint" {
//      install(empty.logic { it.right() })
//      client.invoke(Endpoint.post(), "http://localhost:8080", Unit) shouldBe DecodeResult.Value(Unit.right())
//    }

//    testServer(in_query_out_string, "with URL encoding")((fruit: String) => pureResult(s"fruit: $fruit".asRight[Unit])) { baseUri =>
//      basicRequest.get(uri"$baseUri?fruit=red%20apple").send(backend).map(_.body shouldBe Right("fruit: red apple"))
//    },
//    testServer[String, Nothing, String](in_query_out_infallible_string)((fruit: String) => pureResult(s"fruit: $fruit".asRight[Nothing])) {
//      baseUri =>
//      basicRequest.get(uri"$baseUri?fruit=kiwi").send(backend).map(_.body shouldBe Right("fruit: kiwi"))
//    },
//    testServer(in_query_query_out_string) { case (fruit: String, amount: Option[Int]) => pureResult(s"$fruit $amount".asRight[Unit]) } {
//      baseUri =>
//      basicRequest.get(uri"$baseUri?fruit=orange").send(backend).map(_.body shouldBe Right("orange None")) *>
//      basicRequest.get(uri"$baseUri?fruit=orange&amount=10").send(backend).map(_.body shouldBe Right("orange Some(10)"))
//    },
//    testServer(in_header_out_string)((p1: String) => pureResult(s"$p1".asRight[Unit])) { baseUri =>
//      basicRequest.get(uri"$baseUri").header("X-Role", "Admin").send(backend).map(_.body shouldBe Right("Admin"))
//    },

    listOf(
      Tuple4(Endpoint.get().logic { it.right() }, "GET a GET endpoint", Unit, Unit.right()),
      Tuple4(TestEndpoint.in_query_out_string.logic { it.right() }, null, "orange", "orange".right()),
//      Tuple4(TestEndpoint.in_query_out_string.logic { it.right() }, "URL encoding", "red apple", "red apple".right()),
      Tuple4(empty.logic { it.right() }, "GET empty endpoint", Unit, Unit.right()),
    ).forEach { (s, postfix, input, expected) ->
      "${s.endpoint.details()} $postfix" {
        install(s)

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

        client.invoke((s.endpoint as Endpoint<Any?, Any?, Any?>), "http://localhost:8080", input).map(::adjust) shouldBe DecodeResult.Value(
          adjust(
            expected
          )
        )
      }
    }
  }
}
