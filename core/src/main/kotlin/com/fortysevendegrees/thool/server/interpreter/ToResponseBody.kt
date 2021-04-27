package com.fortysevendegrees.thool.server.interpreter

import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import java.io.InputStream
import java.nio.ByteBuffer
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
    override fun toByteArray(): kotlin.ByteArray = byteBuffer.moveToByteArray()
  }
  public inline class InputStream(public val inputStream: java.io.InputStream) : Body {
    override fun toByteArray(): kotlin.ByteArray = inputStream.readBytes()
  }
}

private fun ByteBuffer.moveToByteArray(): ByteArray {
  val array = ByteArray(remaining())
  get(array)
  return array
}

public interface ToResponseBody<B> {

  fun fromRawValue(
    raw: Body,
    headers: HasHeaders,
    format: CodecFormat
  ): B // TODO: remove headers?

//  fun <REQ, RESP> fromWebSocketPipe(pipe: (REQ) -> RESP, o: WebSocketBodyOutput[streams.Pipe[REQ, RESP], REQ, RESP, _, S]): B
}
