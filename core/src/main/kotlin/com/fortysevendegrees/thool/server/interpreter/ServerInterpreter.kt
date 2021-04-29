package com.fortysevendegrees.thool.server.interpreter

import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.EndpointInterceptor
import com.fortysevendegrees.thool.EndpointOutput
import arrow.core.Either
import arrow.core.tail
import com.fortysevendegrees.thool.Params
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.ServerRequest
import com.fortysevendegrees.thool.model.ServerResponse
import com.fortysevendegrees.thool.model.StatusCode
import com.fortysevendegrees.thool.server.ServerEndpoint

public class ServerInterpreter(
  public val request: ServerRequest,
  public val requestBody: RequestBody,
  public val interceptors: List<EndpointInterceptor>
) {

  public tailrec suspend operator fun <I, E, O> invoke(ses: List<ServerEndpoint<I, E, O>>): ServerResponse? =
    if (ses.isEmpty()) null
    else {
      invoke(ses.first()) ?: invoke(ses.tail())
    }

  public suspend operator fun <I, E, O> invoke(se: ServerEndpoint<I, E, O>): ServerResponse? {
    val valueToResponse: suspend (i: I) -> ServerResponse = { i ->
      when (val res = se.logic(i)) {
        is Either.Left -> outputToResponse(StatusCode.BadRequest, se.endpoint.errorOutput, res.value)
        is Either.Right -> outputToResponse(StatusCode.Ok, se.endpoint.output, res.value)
      }
    }

    val decodedBasicInputs = DecodeBasicInputs.apply(se.endpoint.input, request)

    return when (val values = decodeBody(decodedBasicInputs)) {
      is DecodeBasicInputsResult.Values ->
        when (val res = InputValueResult.from(se.endpoint.input, values)) {
          is InputValueResult.Value ->
            callInterceptorsOnDecodeSuccess(interceptors, se.endpoint, res.params.asAny as I, valueToResponse)
          is InputValueResult.Failure -> callInterceptorsOnDecodeFailure(
            interceptors,
            se.endpoint,
            res.input,
            res.failure
          )
        }
      is DecodeBasicInputsResult.Failure -> callInterceptorsOnDecodeFailure(
        interceptors,
        se.endpoint,
        values.input,
        values.failure
      )
    }
  }

  private suspend fun <I> callInterceptorsOnDecodeSuccess(
    interceptors: List<EndpointInterceptor>,
    endpoint: Endpoint<I, *, *>,
    i: I,
    callLogic: suspend (I) -> ServerResponse
  ): ServerResponse =
    interceptors.firstOrNull()?.onDecodeSuccess(request, endpoint, i) { output ->
      when (output) {
        null -> callInterceptorsOnDecodeSuccess(interceptors.tail(), endpoint, i, callLogic)
        else -> outputToResponse(StatusCode.Ok, output.output as EndpointOutput<Any?>, output.value)
      }
    } ?: callLogic(i)

  private suspend fun callInterceptorsOnDecodeFailure(
    interceptors: List<EndpointInterceptor>,
    endpoint: Endpoint<*, *, *>,
    failingInput: EndpointInput<*>,
    failure: DecodeResult.Failure
  ): ServerResponse? =
    interceptors.firstOrNull()?.onDecodeFailure(request, endpoint, failure, failingInput) { output ->
      when (output) {
        null -> callInterceptorsOnDecodeFailure(interceptors.tail(), endpoint, failingInput, failure)
        else -> outputToResponse(StatusCode.BadRequest, output.output as EndpointOutput<Any?>, output.value)
      }
    }

  private suspend fun decodeBody(result: DecodeBasicInputsResult): DecodeBasicInputsResult =
    when (result) {
      is DecodeBasicInputsResult.Values ->
        when (result.bodyInputWithIndex) {
          null -> result
          else -> {
            val body = result.bodyInputWithIndex.first
            val raw = requestBody.toRaw(body)
            val codec = body.codec as Codec<Any?, Any, CodecFormat>
            when (val res = codec.decode(raw)) {
              is DecodeResult.Value -> result.setBodyInputValue(res.value)
              is DecodeResult.Failure -> DecodeBasicInputsResult.Failure(body, res)
            }
          }
        }
      is DecodeBasicInputsResult.Failure -> result
    }

  private fun <O> outputToResponse(defaultStatusCode: StatusCode, output: EndpointOutput<O>, v: O): ServerResponse {
    val outputValues = OutputValues.of(
      output,
      Params.ParamsAsAny(v),
      OutputValues.empty()
    )
    val statusCode = outputValues.statusCode ?: defaultStatusCode

    val headers = outputValues.headers()

    return ServerResponse(statusCode, headers, outputValues.body)
  }
}
