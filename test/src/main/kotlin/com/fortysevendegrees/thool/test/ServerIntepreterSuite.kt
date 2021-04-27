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
import com.fortysevendegrees.thool.Thool.fixedPath
import com.fortysevendegrees.thool.Thool.stringBody
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

public abstract class ServerIntepreterSuite<Ctx> : FreeSpec() {

  private val client = ApacheClient()

  public abstract suspend fun <A> withEndpoint(
    endpoint: ServerEndpoint<*, *, *>,
    run: suspend Ctx.(baseUrl: String) -> A
  ): A

  public open suspend fun <I, E, O> Ctx.request(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): DecodeResult<Either<E, O>> =
    client.invoke(endpoint, baseUrl, input)

  init {
    val empty = Endpoint(EndpointInput.empty(), EndpointOutput.empty(), EndpointOutput.empty(), EndpointInfo.empty())
    val emptyGet = empty.input(Thool.method(Method.GET))
    val emptyPost = empty.input(Thool.method(Method.POST))

    "POST a GET endpoint" {
      withEndpoint(emptyGet.logic { it.right() }) { baseUrl ->
        request(emptyPost, baseUrl, Unit) shouldBe DecodeResult.Value(Unit.left())
      }
    }

    "POST empty endpoint" {
      withEndpoint(empty.logic { it.right() }) { baseUrl ->
        request(emptyPost, baseUrl, Unit) shouldBe DecodeResult.Value(Unit.right())
      }
    }

    "Empty path should not be passed to path capture decoding" {
      withEndpoint(TestEndpoint.in_path.logic { Unit.right() }) { baseUrl ->
        request(emptyGet.input(fixedPath("api")), baseUrl, Unit) shouldBe DecodeResult.Value(Unit.left())
      }
    }

    "Calling post \"api\" / \"echo\" with get" {
      withEndpoint(TestEndpoint.in_string_out_string.logic { it.right() }) { baseUrl ->
        request(emptyGet.input(stringBody()), baseUrl, "Sweet") shouldBe DecodeResult.Value(Unit.left())
      }
    }

    listOf(
      Tuple4(Endpoint.get().logic { it.right() }, "GET a GET endpoint", Unit, Unit.right()),
      Tuple4(TestEndpoint.in_query_out_string.logic { it.right() }, null, "orange", "orange".right()),
      Tuple4(TestEndpoint.in_query_out_string.logic { it.right() }, "URL encoding", "red apple", "red apple".right()),
      Tuple4(empty.logic { it.right() }, "GET empty endpoint", Unit, Unit.right()),
      Tuple4(
        TestEndpoint.in_query_out_infallible_string.logic { "fruit: $it".right() },
        null,
        "kiwi",
        "fruit: kiwi".right()
      ),
      Tuple4(
        TestEndpoint.in_query_query_out_string.logic { (f, a) -> "$f: $a".right() },
        "Defined",
        Pair("orange", 10),
        "orange: 10".right()
      ),
      Tuple4(
        TestEndpoint.in_query_query_out_string.logic { (f, a) -> "$f: $a".right() },
        "Null",
        Pair("orange", null),
        "orange: null".right()
      ),
      Tuple4(
        TestEndpoint.in_header_out_string.logic { it.right() },
        null,
        "Admin",
        "Admin".right()
      ),
      Tuple4(
        TestEndpoint.in_path_path_out_string.logic { (fruit, amount) -> "$fruit: $amount".right() },
        null,
        Pair("orange", 20),
        "orange: 20".right()
      ),
//      TODO fix URL encoding => expected: "apple/red: 20" but was: "apple%2Fred: 20"
//      Tuple4(
//        TestEndpoint.in_path_path_out_string.logic { (fruit, amount) -> "$fruit: $amount".right() },
//        "URL encoding",
//        Pair("apple/red", 20),
//        "apple/red: 20".right()
//      ),
      Tuple4(
        TestEndpoint.in_two_path_capture.logic { it.right() },
        "capturing two path parameters with the same specification",
        Pair(12, 23),
        Pair(12, 23).right()
      ),
      Tuple4(
        TestEndpoint.in_string_out_string.logic { it.right() },
        null,
        "Sweet",
        "Sweet".right()
      ),
      Tuple4(
        TestEndpoint.in_mapped_query_out_string.logic { l -> "length: ${l.size}".right() },
        null,
        "orange".toList(),
        "length: 6".right()
      ),
      Tuple4(
        TestEndpoint.in_mapped_path_out_string.logic { it.name.right() },
        null,
        Fruit("kiwi"),
        "kiwi".right()
      ),
      Tuple4(
        TestEndpoint.in_mapped_path_path_out_string.logic { (f, a) -> "FA: FruitAmount($f, $a)".right() },
        null,
        FruitAmount("orange", 10),
        "FA: FruitAmount(orange, 10)".right()
      ),
      Tuple4(
        TestEndpoint.in_query_mapped_path_path_out_string.logic { (fa, color) -> "FA: FruitAmount(${fa.fruit}, ${fa.amount}) color: $color".right() },
        null,
        Pair(FruitAmount("orange", 10), "yellow"),
        "FA: FruitAmount(orange, 10) color: yellow".right()
      ),
      Tuple4(
        TestEndpoint.in_query_out_mapped_string.logic { it.toList().right() },
        null,
        "orange",
        "orange".toList().right()
      )
    ).forEach { (s, postfix, input, expected) ->
      "${s.endpoint.details()} $postfix" {
        withEndpoint(s) { baseUrl ->
          request((s.endpoint as Endpoint<Any?, Any?, Any?>), baseUrl, input)
            .map(::adjust) shouldBe DecodeResult.Value(adjust(expected))
        }
      }
    }
  }
}

internal fun adjust(r: Either<Any?, Any?>): Either<Any?, Any?> {
  fun doAdjust(v: Any?): Any? = when (v) {
    is InputStream -> v.readBytes().toList()
    is ByteArray -> v.toList()
    is ByteBuffer -> v.array().toList()
    is ByteArrayInputStream -> v.readBytes().toList()
    else -> v
  }

  return r.bimap(::doAdjust, ::doAdjust)
}
