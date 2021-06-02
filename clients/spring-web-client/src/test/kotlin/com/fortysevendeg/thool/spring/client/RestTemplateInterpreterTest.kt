package com.fortysevendeg.thool.spring.client

import arrow.core.Either
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.model.StatusCode
import com.fortysevendeg.thool.test.ClientInterpreterSuite
import org.springframework.web.client.RestTemplate

class RestTemplateInterpreterTest : ClientInterpreterSuite() {

  private val client = RestTemplate()

//  override suspend fun <I, E, O> request(endpoint: Endpoint<I, E, O>, baseUrl: String, input: I): DecodeResult<Either<E, O>> {
//    val f = endpoint.toRequestAndParseRestTemplate(baseUrl)
//    return client.f(input).second
//  }

  override suspend fun <I, E, O> requestAndStatusCode(
    endpoint: Endpoint<I, E, O>,
    baseUrl: String,
    input: I
  ): Pair<DecodeResult<Either<E, O>>, StatusCode> {
    val (res, response) = client.invokeAndResponse(endpoint, baseUrl, input)
    return Pair(res, StatusCode(response.rawStatusCode))
  }
}
