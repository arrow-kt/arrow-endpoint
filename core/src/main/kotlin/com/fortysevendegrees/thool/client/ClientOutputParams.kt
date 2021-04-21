package com.fortysevendegrees.thool.client

import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.CombineParams
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointOutput
import com.fortysevendegrees.thool.Mapping
import com.fortysevendegrees.thool.Params
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.ResponseMetadata
import java.lang.IllegalArgumentException

/**
 * TODO: Add Client websocket support fun decodeWebSocketBody(o: WebSocketBodyOutput<*, *, *, *, *>, body: Any): DecodeResult<Any>
 */
@Suppress("UNCHECKED_CAST")
fun EndpointOutput<*>.decode(body: Any, meta: ResponseMetadata): DecodeResult<Params> =
  when (this) {
    is EndpointIO.Pair<*, *, *> -> resolveOutputPair(first, second, combine, body, meta)
    is EndpointOutput.Pair<*, *, *> -> resolveOutputPair(first, second, combine, body, meta)
    is EndpointOutput.Single<*> -> {
      val r: DecodeResult<Any?> =
        when (this) {
          is EndpointIO.Body<*, *> -> (codec as Codec<Any?, Any?, CodecFormat>).decode(body)
          is EndpointIO.Empty -> codec.decode(Unit)
          is EndpointIO.Header -> codec.decode(meta.headers(name))
          is EndpointIO.StreamBody -> TODO("How to get the Byte from body?")
          is EndpointIO.MappedPair<*, *, *, *> ->
            wrapped.decode(body, meta)
              .flatMap { (mapping as Mapping<Any?, Any?>).decode(it.asAny) }
          is EndpointOutput.MappedPair<*, *, *, *> ->
            output.decode(body, meta)
              .flatMap { (mapping as Mapping<Any?, Any?>).decode(it.asAny) }
          is EndpointOutput.FixedStatusCode -> codec.decode(Unit)
          is EndpointOutput.StatusCode -> codec.decode(meta.code)
        }
      r.map { Params.ParamsAsAny(it) }
    }
    is EndpointOutput.Void ->
      DecodeResult.Failure.Error("", IllegalArgumentException("Cannot convert a void output to a value!"))
  }

private fun resolveOutputPair(
  first: EndpointOutput<*>,
  second: EndpointOutput<*>,
  combine: CombineParams,
  body: Any,
  meta: ResponseMetadata
): DecodeResult<Params> {
  val a: DecodeResult<Params> = first.decode(body, meta)
  val b: DecodeResult<Params> = second.decode(body, meta)
  return a.flatMap { firstParams: Params ->
    b.map { secondParams: Params ->
      combine(firstParams, secondParams)
    }
  }
}
