package com.fortysevendegrees.thool.spring.client

import arrow.core.Either
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.test.ClientInterpreterSuite
import org.springframework.web.reactive.function.client.WebClient

class WebClientInterpreterTest : ClientInterpreterSuite() {

  private val client = WebClient.create()

  override suspend fun <I, E, O> request(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): DecodeResult<Either<E, O>> {
    val f = endpoint.toRequestAndParseWebClient(baseUrl)
    return client.f(input).second
  }
}
