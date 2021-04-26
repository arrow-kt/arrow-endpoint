package com.fortysevendegrees.thool.server.interpreter

import kotlinx.coroutines.flow.Flow
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import com.fortysevendegrees.thool.server.intrepreter.Body
import java.nio.charset.Charset

public interface ToResponseBody<B> {

  fun fromRawValue(
    raw: Body,
    headers: HasHeaders,
    format: CodecFormat
  ): B // TODO: remove headers?

  fun fromStreamValue(raw: Flow<Byte>, headers: HasHeaders, format: CodecFormat, charset: Charset?): B

//  fun <REQ, RESP> fromWebSocketPipe(pipe: (REQ) -> RESP, o: WebSocketBodyOutput[streams.Pipe[REQ, RESP], REQ, RESP, _, S]): B
}
