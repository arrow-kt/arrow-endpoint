package arrow.endpoint.model

import java.io.InputStream as JInputStream
import java.nio.ByteBuffer as JByteBuffer
import kotlin.ByteArray as KByteArray

public data class ByteBuffer(public val byteBuffer: JByteBuffer, public override val format: CodecFormat) :
  Body {
  override fun toByteArray(): KByteArray {
    val array = KByteArray(byteBuffer.remaining())
    byteBuffer.get(array)
    return array
  }
}

public data class InputStream(public val inputStream: JInputStream, public override val format: CodecFormat) :
  Body {
  override fun toByteArray(): KByteArray = inputStream.readBytes()
}
