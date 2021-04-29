package com.fortysevendegrees.thool.server.interpreter

import com.fortysevendegrees.thool.model.Body
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.Header

public interface ToResponseBody<B> {

  fun fromRawValue(
    raw: Body,
    headers: List<Header>,
    format: CodecFormat
  ): B // TODO: remove headers?

//  fun <REQ, RESP> fromWebSocketPipe(pipe: (REQ) -> RESP, o: WebSocketBodyOutput[streams.Pipe[REQ, RESP], REQ, RESP, _, S]): B
}
