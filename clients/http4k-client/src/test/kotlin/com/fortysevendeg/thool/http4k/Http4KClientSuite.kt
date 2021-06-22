package com.fortysevendeg.thool.http4k

import arrow.core.Either
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.http4k.client.execute
import com.fortysevendeg.thool.model.StatusCode
import com.fortysevendeg.thool.test.ClientInterpreterSuite
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler

class Http4KClientSuite : ClientInterpreterSuite() {

  private val client: HttpHandler = ApacheClient()

  override suspend fun <I, E, O> requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (_, resp, res) = client.execute(endpoint, baseUrl, input)
    return Pair(res, StatusCode(resp.status.code))
  }
}
