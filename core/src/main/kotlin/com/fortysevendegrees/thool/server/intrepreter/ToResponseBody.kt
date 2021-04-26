package com.fortysevendegrees.thool.server.intrepreter

import kotlinx.coroutines.flow.Flow
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import java.nio.charset.Charset

public sealed interface Body {
  fun toByteArray(): kotlin.ByteArray

  public data class String(public val charset: Charset, public val string: kotlin.String) : Body {
    override fun toByteArray(): kotlin.ByteArray = string.toByteArray(charset)
  }

  public inline class ByteArray(public val byteArray: kotlin.ByteArray) : Body {
    override fun toByteArray(): kotlin.ByteArray = byteArray
  }

  public inline class ByteBuffer(public val byteBuffer: java.nio.ByteBuffer) : Body {
    override fun toByteArray(): kotlin.ByteArray = byteBuffer.array()
  }

  public inline class InputStream(public val inputStream: java.io.InputStream) : Body {
    override fun toByteArray(): kotlin.ByteArray = inputStream.readBytes()
  }
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
