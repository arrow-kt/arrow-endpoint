package com.fortysevendeg.thool.ktor.client

import arrow.core.Either
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.test.ClientInterpreterSuite
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

class KtorClientSuite : ClientInterpreterSuite() {

  private val client = HttpClient(CIO)

  override suspend fun <I, E, O> request(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): DecodeResult<Either<E, O>> {
    val f = endpoint.requestAndParse(baseUrl)
    return client.f(input)
  }
}
