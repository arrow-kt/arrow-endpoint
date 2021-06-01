package com.fortysevendeg.thool.server.interpreter

import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.EndpointOutput
import com.fortysevendeg.thool.Mapping
import com.fortysevendeg.thool.Params
import com.fortysevendeg.thool.SplitParams
import com.fortysevendeg.thool.model.Body
import com.fortysevendeg.thool.model.CodecFormat
import com.fortysevendeg.thool.model.Header
import com.fortysevendeg.thool.model.MediaType
import com.fortysevendeg.thool.model.StatusCode
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

internal data class OutputValues(
  val body: Body?,
  val baseHeaders: List<Header>,
  val headerTransformations: List<(List<Header>) -> List<Header>>,
  val statusCode: StatusCode?
) {
  private fun withBody(b: Body): OutputValues {
    check(body == null) { "Body is already defined" }
    return copy(body = b)
  }

  private fun withHeaderTransformation(t: (List<Header>) -> List<Header>): OutputValues =
    copy(headerTransformations = headerTransformations + t)

  private fun withDefaultContentType(format: CodecFormat, charset: Charset?): OutputValues =
    withHeaderTransformation { hs ->
      if (hs.any { it.hasName(Header.ContentType) }) hs
      else hs + Header(
        Header.ContentType,
        (charset?.let(format.mediaType::charset) ?: format.mediaType).toString()
      )
    }

  private fun withHeader(n: String, v: String): OutputValues =
    copy(baseHeaders = baseHeaders + Header(n, v))

  private fun withStatusCode(sc: StatusCode): OutputValues =
    copy(statusCode = sc)

  fun headers(): List<Header> =
    headerTransformations.fold(baseHeaders) { hs, t -> t(hs) }

  public companion object {
    fun empty(): OutputValues =
      OutputValues(null, emptyList(), emptyList(), null)

    fun of(
      output: EndpointOutput<*>,
      params: Params,
      ov: OutputValues
    ): OutputValues =
      when (output) {
        is EndpointIO.Single<*> -> applySingle(output, params, ov)
        is EndpointOutput.Single<*> -> applySingle(output, params, ov)
        is EndpointOutput.Pair<*, *, *> -> applyPair(
          output.first,
          output.second,
          output.split,
          params,
          ov
        )
        is EndpointIO.Pair<*, *, *> -> applyPair(
          output.first,
          output.second,
          output.split,
          params,
          ov
        )
        is EndpointOutput.Void -> throw IllegalArgumentException("Cannot encode a void output!")
      }

    private fun OutputValues.withBody(
      body: Body,
      output: EndpointIO.Body<*, *>
    ): OutputValues =
      withBody(body).withDefaultContentType(output.codec.format, charset(output.codec.format.mediaType, output))

    private fun applySingle(
      output: EndpointOutput.Single<*>,
      value: Params,
      ov: OutputValues
    ): OutputValues =
      when (output) {
        is EndpointIO.Empty -> ov
        is EndpointOutput.FixedStatusCode -> ov.withStatusCode(output.statusCode)
        is EndpointIO.Header -> {
          val mapping = output.codec as Mapping<List<String>, Any?>
          mapping.encode(value.asAny).fold(ov) { ovv, headerValue ->
            ovv.withHeader(output.name, headerValue)
          }
        }
        is EndpointIO.ByteArrayBody -> {
          val mapping = output.codec as Mapping<ByteArray, Any?>
          ov.withBody(Body.ByteArray(mapping.encode(value.asAny), output.codec.format), output)
        }
        is EndpointIO.ByteBufferBody -> {
          val mapping = output.codec as Mapping<ByteBuffer, Any?>
          ov.withBody(Body.ByteBuffer(mapping.encode(value.asAny), output.codec.format), output)
        }
        is EndpointIO.InputStreamBody -> {
          val mapping = output.codec as Mapping<InputStream, Any?>
          ov.withBody(Body.InputStream(mapping.encode(value.asAny), output.codec.format), output)
        }
        is EndpointIO.StringBody -> {
          val mapping = output.codec as Mapping<String, Any?>
          ov.withBody(Body.String(output.charset, mapping.encode(value.asAny), output.codec.format), output)
        }
        is EndpointOutput.StatusCode -> {
          val mapping = output.codec as Mapping<StatusCode, Any?>
          ov.withStatusCode(mapping.encode(value.asAny))
        }
        is EndpointOutput.OneOf<*, *> -> {
          val encoded = (output.codec as Mapping<Any?, Any?>).encode(value.asAny)
          val mapping = output.mappings
            .firstOrNull { it.appliesTo(encoded) }
            ?: throw IllegalArgumentException("No status code mapping for value: $encoded, in output: $output")
          of(mapping.output, Params.ParamsAsAny(encoded), mapping.statusCode?.let(ov::withStatusCode) ?: ov)
        }
        is EndpointIO.MappedPair<*, *, *, *> -> {
          val mapping = output.mapping as Mapping<Any?, Any?>
          of(output.wrapped, Params.ParamsAsAny(mapping.encode(value.asAny)), ov)
        }
        is EndpointOutput.MappedPair<*, *, *, *> -> {
          val mapping = output.mapping as Mapping<Any?, Any?>
          of(output.output, Params.ParamsAsAny(mapping.encode(value.asAny)), ov)
        }
      }

    private fun applyPair(
      left: EndpointOutput<*>,
      right: EndpointOutput<*>,
      split: SplitParams,
      params: Params,
      ov: OutputValues
    ): OutputValues {
      val (leftParams, rightParams) = split(params)
      return of(
        right, rightParams,
        of(
          left, leftParams, ov
        )
      )
    }

    private fun charset(mediaType: MediaType, body: EndpointIO.Body<*, *>): Charset? =
      when (body) {
        // TODO: add to MediaType - setting optional charset if text
        is EndpointIO.StringBody -> if (mediaType.mainType.equals("text", ignoreCase = true)) body.charset else null
        else -> null
      }
  }
}
