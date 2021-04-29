package com.fortysevendegrees.thool.model

import java.nio.charset.Charset

public data class Address(val hostname: String, val port: Int)
public data class ConnectionInfo(val local: Address?, val remote: Address?, val secure: Boolean?)

public data class ServerRequest(
  val protocol: String,
  val connectionInfo: ConnectionInfo,
  public val method: Method,
  public val uri: Uri,
  public val headers: List<Header>,
  /**
   * Can differ from `uri.path()`, if the endpoint is deployed in a context.
   * If the routes are mounted within a context (e.g. using a router), we have to match against what comes after the context.
   */
  public val pathSegments: List<String>,
  public val queryParameters: QueryParams
) {
  override fun toString(): String =
    "ServerRequest($protocol, $connectionInfo, $method, $uri, ${headers.toStringSafe()})"
}

public sealed interface Body {
  public fun toByteArray(): kotlin.ByteArray
  public data class String(public val charset: Charset, public val string: kotlin.String) : Body {
    override fun toByteArray(): kotlin.ByteArray = string.toByteArray(charset)
  }

  public inline class ByteArray(public val byteArray: kotlin.ByteArray) : Body {
    override fun toByteArray(): kotlin.ByteArray = byteArray
  }

  public inline class ByteBuffer(public val byteBuffer: java.nio.ByteBuffer) : Body {
    override fun toByteArray(): kotlin.ByteArray {
      val array = kotlin.ByteArray(byteBuffer.remaining())
      byteBuffer.get(array)
      return array
    }
  }

  public inline class InputStream(public val inputStream: java.io.InputStream) : Body {
    override fun toByteArray(): kotlin.ByteArray = inputStream.readBytes()
  }
}

public data class ServerResponse(
  val code: StatusCode,
  val statusText: String,
  val headers: List<Header>,
  val body: Body?
) {
  override fun toString(): String = "ServerResponse($code, $statusText, ${headers.toStringSafe()}, $body)"
}
