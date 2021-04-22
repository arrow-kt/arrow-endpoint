package com.fortysevendegrees.thool.ktor.client

import arrow.core.Either
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.test.ClientInterpreterSuite
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

class InterpreterKtTest : ClientInterpreterSuite() {

  private val client = HttpClient(CIO)

  override suspend fun <I, E, O> request(endpoint: Endpoint<I, E, O>, baseUrl: String, input: I): DecodeResult<Either<E, O>> {
    val f = endpoint.requestAndParse(baseUrl)
    return client.f(input)
  }
}
