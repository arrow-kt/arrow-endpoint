package com.fortysevendegrees.thool.server.interpreter

import kotlinx.coroutines.flow.Flow
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

public sealed interface Body
public data class StringBody(public val charset: Charset, public val string: String) : Body
public inline class ByteArrayBody(public val byteArray: ByteArray) : Body
public inline class ByteBufferBody(public val byteBuffer: ByteBuffer) : Body
public inline class InputStreamBody(public val inputStream: InputStream) : Body

public interface ToResponseBody<B> {

  fun fromRawValue(
    raw: Body,
    headers: HasHeaders,
    format: CodecFormat
  ): B // TODO: remove headers?

  fun fromStreamValue(raw: Flow<Byte>, headers: HasHeaders, format: CodecFormat, charset: Charset?): B

//  fun <REQ, RESP> fromWebSocketPipe(pipe: (REQ) -> RESP, o: WebSocketBodyOutput[streams.Pipe[REQ, RESP], REQ, RESP, _, S]): B
}
