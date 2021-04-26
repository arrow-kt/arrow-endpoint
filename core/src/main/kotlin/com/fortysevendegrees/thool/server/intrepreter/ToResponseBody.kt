package com.fortysevendegrees.thool.server.intrepreter

import kotlinx.coroutines.flow.Flow
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

public sealed interface Body {
  fun toByteArray(): ByteArray
}

public data class StringBody(public val charset: Charset, public val string: String) : Body {
  override fun toByteArray(): ByteArray = string.toByteArray(charset)
}

public inline class ByteArrayBody(public val byteArray: ByteArray) : Body {
  override fun toByteArray(): ByteArray = byteArray
}

public inline class ByteBufferBody(public val byteBuffer: ByteBuffer) : Body {
  override fun toByteArray(): ByteArray = byteBuffer.array()
}

public inline class InputStreamBody(public val inputStream: InputStream) : Body {
  override fun toByteArray(): ByteArray = inputStream.readBytes()
}

public interface ToResponseBody<B> {

  fun fromRawValue(
    v: Body,
    headers: HasHeaders,
    format: CodecFormat
  ): B // TODO: remove headers?

  fun fromStreamValue(v: Flow<Byte>, headers: HasHeaders, format: CodecFormat, charset: Charset?): B

//  fun <REQ, RESP> fromWebSocketPipe(pipe: (REQ) -> RESP, o: WebSocketBodyOutput[streams.Pipe[REQ, RESP], REQ, RESP, _, S]): B
}
