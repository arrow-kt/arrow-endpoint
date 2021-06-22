package com.fortysevendeg.thool.spring.client

import arrow.core.Either
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.model.StatusCode
import com.fortysevendeg.thool.test.ClientInterpreterSuite
import org.springframework.web.reactive.function.client.WebClient

class WebClientInterpreterTest : ClientInterpreterSuite() {

  private val client = WebClient.create()

  override suspend fun <I, E, O> requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (_, resp, res) = client.execute(endpoint, baseUrl, input)
    return Pair(res, StatusCode(resp.rawStatusCode()))
  }
}
