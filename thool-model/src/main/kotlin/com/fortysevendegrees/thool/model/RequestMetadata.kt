package com.fortysevendegrees.thool.model

import java.nio.charset.Charset
import java.io.InputStream as JInputStream
import java.nio.ByteBuffer as JByteBuffer
import kotlin.ByteArray as KByteArray
import kotlin.String as KString

public data class Address(val hostname: KString, val port: Int)
public data class ConnectionInfo(val local: Address?, val remote: Address?, val secure: Boolean?)

public data class ServerRequest(
  val protocol: KString,
  val connectionInfo: ConnectionInfo,
  public val method: Method,
  public val uri: Uri,
  public val headers: List<Header>,
  /**
   * Can differ from `uri.path()`, if the endpoint is deployed in a context.
   * If the routes are mounted within a context (e.g. using a router), we have to match against what comes after the context.
   */
  public val pathSegments: List<KString>,
  public val queryParameters: QueryParams
) {
  override fun toString(): KString =
    "ServerRequest($protocol, $connectionInfo, $method, $uri, ${headers.toStringSafe()})"
}

public sealed interface Body {
  public fun toByteArray(): KByteArray
  public val format: CodecFormat

  public fun Body.charsetOrNull(): Charset? =
    when (this) {
      is String -> charset
      else -> null
    }

  public data class String(
    public val charset: Charset,
    public val string: KString,
    public override val format: CodecFormat
  ) : Body {
    override fun toByteArray(): KByteArray = string.toByteArray(charset)
  }

  public data class ByteArray(public val byteArray: KByteArray, public override val format: CodecFormat) : Body {
    override fun toByteArray(): KByteArray = byteArray
  }

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
}

public data class ServerResponse(
  val code: StatusCode,
  val headers: List<Header>,
  val body: Body?
) {
  override fun toString(): KString = "ServerResponse($code, ${headers.toStringSafe()}, $body)"
}
