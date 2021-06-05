package com.fortysevendeg.thool.http4k

import arrow.core.Either
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.model.StatusCode
import com.fortysevendeg.thool.test.ClientInterpreterSuite
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler

class Http4KClientSuite : ClientInterpreterSuite() {

  val client: HttpHandler = ApacheClient()

  override suspend fun <I, E, O> requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (request, parser) = endpoint.toRequestAndParser(baseUrl)(input)
    val response = client(request)
    return Pair(parser(response), StatusCode(response.status.code))
  }
}
