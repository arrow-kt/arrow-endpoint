package arrow.endpoint.model

import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.toByteArray
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
  public fun toByteArray(): kotlin.ByteArray
  public val format: CodecFormat

  public fun Body.charsetOrNull(): Charset? =
    when (this) {
      is String -> charset
      else -> null
    }

  public data class String(
    public val charset: Charset,
    public val string: kotlin.String,
    public override val format: CodecFormat
  ) : Body {
    override fun toByteArray(): kotlin.ByteArray = string.toByteArray(charset)
  }

  public data class ByteArray(public val byteArray: kotlin.ByteArray, public override val format: CodecFormat) : Body {
    override fun toByteArray(): kotlin.ByteArray = byteArray

    override fun equals(other: Any?): Boolean {
      return when {
        other == null -> false
        this === other -> true
        this::class != other::class -> false
        else -> {
          other as ByteArray

          if (format != other.format) return false
          if (!byteArray.contentEquals(other.byteArray)) return false
          true
        }
      }
    }

    override fun hashCode(): Int {
      var result = byteArray.contentHashCode()
      result = 31 * result + format.hashCode()
      return result
    }
  }
}

public data class ServerResponse(
  val code: StatusCode,
  val headers: List<Header>,
  val body: Body?
) {
  override fun toString(): KString = "ServerResponse($code, ${headers.toStringSafe()}, $body)"
}
