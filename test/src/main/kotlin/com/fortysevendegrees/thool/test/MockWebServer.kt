package com.fortysevendegrees.thool.test

import com.fortysevendegrees.thool.ConnectionInfo
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.ServerRequest
import com.fortysevendegrees.thool.map
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.model.HasHeaders
import com.fortysevendegrees.thool.model.Header
import com.fortysevendegrees.thool.model.HeaderNames
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.QueryParams
import com.fortysevendegrees.thool.model.StatusCode
import com.fortysevendegrees.thool.model.Uri
import com.fortysevendegrees.thool.server.ServerEndpoint
import com.fortysevendegrees.thool.server.interpreter.Body
import com.fortysevendegrees.thool.server.interpreter.ByteArrayBody
import com.fortysevendegrees.thool.server.interpreter.ByteBufferBody
import com.fortysevendegrees.thool.server.interpreter.InputStreamBody
import com.fortysevendegrees.thool.server.interpreter.RequestBody
import com.fortysevendegrees.thool.server.interpreter.ServerInterpreter
import com.fortysevendegrees.thool.server.interpreter.StringBody
import com.fortysevendegrees.thool.server.interpreter.ToResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.nio.ByteBuffer
import java.nio.charset.Charset

public fun <I, E, O> ServerEndpoint<I, E, O>.toDispatcher(): Dispatcher =
  object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      val serverRequest = ServerRequest(request)
      val interpreter = ServerInterpreter(
        serverRequest,
        RequestBody(request),
        ToResponseBody(),
        emptyList()
      )

      return runBlocking(Dispatchers.Default) {
        interpreter.invoke(this@toDispatcher)?.let {
          when (val body = it.body) {
            null -> MockResponse().setResponseCode(it.code.code)
            else -> body.setResponseCode(it.code.code)
          }
        } ?: MockResponse().setResponseCode(StatusCode.NotFound.code)
      }
    }
  }

internal class RequestBody(val ctx: RecordedRequest) : RequestBody {
  override suspend fun <R> toRaw(bodyType: EndpointIO.Body<R, *>): R {
    return when (bodyType) {
      is EndpointIO.ByteArrayBody -> ctx.body.readByteArray()
      is EndpointIO.ByteBufferBody -> ByteBuffer.wrap(ctx.body.readByteArray())
      is EndpointIO.InputStreamBody -> ctx.body.inputStream()
      is EndpointIO.StringBody -> ctx.body.readByteArray().toString(bodyType.charset)
    } as R
  }

  override fun toFlow(): Flow<Byte> = TODO()
}

internal typealias MockResponseBody = MockResponse

internal class ServerRequest(val ctx: RecordedRequest) : ServerRequest {
  override val protocol: String = "mocked-protocol"
  override val connectionInfo: ConnectionInfo by lazy { ConnectionInfo(null, null, null) }
  override val underlying: Any = ctx

  override val uri: Uri // TODO seems that it can be constructed directly without parsing
    get() = requireNotNull(Uri(ctx.requestUrl?.toUri().toString()))

  override fun pathSegments(): List<String> =
    ctx.requestUrl?.pathSegments.orEmpty()

  override fun queryParameters(): QueryParams =
    ctx.requestUrl?.queryParameterNames
      .orEmpty()
      .map { name -> Pair(name, ctx.requestUrl?.queryParameterValues(name)?.filterNotNull().orEmpty()) }
      .toList()
      .let(::QueryParams)

  override val method: Method =
    Method(requireNotNull(ctx.method) { "No http method specified." })

  override val headers: List<Header> =
    ctx.headers.map { (n, v) -> Header(n, v) }
}

public class ToResponseBody : ToResponseBody<MockResponseBody> {

  override fun fromRawValue(v: Body, headers: HasHeaders, format: CodecFormat): MockResponseBody =
    rawValueToEntity(v, headers, format)

  override fun fromStreamValue(
    v: Flow<Byte>,
    headers: HasHeaders,
    format: CodecFormat,
    charset: Charset?
  ): MockResponseBody = TODO()

  private fun rawValueToEntity(
    r: Body,
    headers: HasHeaders,
    format: CodecFormat,
  ): MockResponseBody =
    when (r) {
      is ByteArrayBody -> MockResponse().setBody(Buffer().apply { read(r.byteArray) })
      is ByteBufferBody -> MockResponse().setBody(Buffer().apply { read(r.byteBuffer) })
      is InputStreamBody -> MockResponse().setBody(Buffer().apply { readFrom(r.inputStream) })
      is StringBody -> MockResponse().setBody(r.string)
    }.addHeader(HeaderNames.ContentType, format.mediaType.toString())
      .apply { headers.headers.forEach { (n, v) -> addHeader(n, v) } }
}
