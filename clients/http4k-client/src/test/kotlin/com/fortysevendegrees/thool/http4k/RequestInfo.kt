package com.fortysevendegrees.thool.http4k

import arrow.core.Tuple6
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.Thool.query
import com.fortysevendegrees.thool.and
import com.fortysevendegrees.thool.client.requestInfo
import com.fortysevendegrees.thool.input
import com.fortysevendegrees.thool.output
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RequestInfo : StringSpec({
  "single query on Endpoint companion" {
    val ps = Endpoint
      .input(query("fruit", Codec.listFirst(Codec.string)))
      .input
      .requestInfo("apple", "http://localhost:8080")
      .queryParams
      .ps

    ps.size shouldBe 1
    println(ps)
  }

  "double query on Endpoint companion" {
    val ps = Endpoint
      .input(query("fruit", Codec.listFirst(Codec.string)).and(query("amount", Codec.listFirst(Codec.int))))
      .input
      .requestInfo(Pair("apple", 10), "http://localhost:8080")
      .queryParams
      .ps

    ps.size shouldBe 2
    println(ps)
  }

  "single query on input extension" {
    val ps = Endpoint
      .get()
      .input(query("fruit", Codec.listFirst(Codec.string)))
      .input
      .requestInfo("apple", "http://localhost:8080")
      .queryParams
      .ps

    ps.size shouldBe 1
    println(ps)
  }

  "double query on input extension" {
    val ps = Endpoint
      .get()
      .input(query("fruit", Codec.listFirst(Codec.string)).and(query("amount", Codec.listFirst(Codec.int))))
      .input
      .requestInfo(Pair("apple", 10), "http://localhost:8080")
      .queryParams
      .ps

    ps.size shouldBe 2
    println(ps)
  }

  "6 query on input extension" {
    val ps =
      Endpoint
        .get()
        .input(
          query("fruit", Codec.listFirst(Codec.string))
            .and(query("amount", Codec.listFirst(Codec.int)))
            .and(query("amount2", Codec.listFirst(Codec.int)))
            .and(query("amount3", Codec.listFirst(Codec.int)))
            .and(query("amount4", Codec.listFirst(Codec.int)))
            .and(query("amount5", Codec.listFirst(Codec.int)))
        )
        .output(Thool.stringBody())
        .input
        .requestInfo(Tuple6("apple", 20, 30, 40, 50, 60), "http://localhost:8080")
        .queryParams
        .ps

    ps.size shouldBe 6
    println(ps)
  }
})
