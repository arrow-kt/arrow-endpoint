package com.fortysevendegrees.tapir.server.intrepreter

import com.fortysevendegrees.tapir.RawBodyType
import kotlinx.coroutines.flow.Flow
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import java.nio.charset.Charset

interface ToResponseBody<B> {

  fun <R> fromRawValue(
    v: R,
    headers: HasHeaders,
    format: CodecFormat,
    bodyType: RawBodyType<R>
  ): B // TODO: remove headers?

  fun fromStreamValue(v: Flow<Byte>, headers: HasHeaders, format: CodecFormat, charset: Charset?): B

//  fun <REQ, RESP> fromWebSocketPipe(pipe: (REQ) -> RESP, o: WebSocketBodyOutput[streams.Pipe[REQ, RESP], REQ, RESP, _, S]): B

}