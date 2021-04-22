package com.fortysevendegrees.thool.test

import com.fortysevendegrees.thool.ConnectionInfo
import com.fortysevendegrees.thool.RawBodyType
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
import com.fortysevendegrees.thool.server.intrepreter.RequestBody
import com.fortysevendegrees.thool.server.intrepreter.ServerInterpreter
import com.fortysevendegrees.thool.server.intrepreter.ToResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

fun <I, E, O> ServerEndpoint<I, E, O>.toDispatcher(): Dispatcher =
  object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      println("###################################################################Getting called with requestUrl: ${request.requestUrl}, path: ${request.path}")

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
            null -> MockResponse().setStatus(it.code.toString())
            else -> body.setStatus(it.code.toString())
          }
        } ?: MockResponse().setStatus(StatusCode.NotFound.toString())
      }.also { println("############################## returned value: $it") }
    }
  }

internal class RequestBody(val ctx: RecordedRequest) : RequestBody {
  override suspend fun <R> toRaw(bodyType: RawBodyType<R>): R {
    return when (bodyType) {
      RawBodyType.ByteArrayBody -> ctx.body.readByteArray()
      RawBodyType.ByteBufferBody -> ByteBuffer.wrap(ctx.body.readByteArray())
      RawBodyType.InputStreamBody -> ctx.body.inputStream()
      is RawBodyType.StringBody -> ctx.body.readByteArray().toString(bodyType.charset)
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

class ToResponseBody : ToResponseBody<MockResponseBody> {

  override fun <R> fromRawValue(
    v: R,
    headers: HasHeaders,
    format: CodecFormat,
    bodyType: RawBodyType<R>
  ): MockResponseBody = rawValueToEntity(bodyType, headers, format, v)

  override fun fromStreamValue(
    v: Flow<Byte>,
    headers: HasHeaders,
    format: CodecFormat,
    charset: Charset?
  ): MockResponseBody = TODO()

  private fun <R> rawValueToEntity(
    bodyType: RawBodyType<R>,
    headers: HasHeaders,
    format: CodecFormat,
    r: R
  ): MockResponseBody =
    when (bodyType) {
      is RawBodyType.StringBody ->
        MockResponse().setBody(r as String)
      RawBodyType.ByteArrayBody ->
        MockResponse()
          .setBody(Buffer().apply { read(r as ByteArray) })
      RawBodyType.InputStreamBody ->
        MockResponse()
          .setBody(Buffer().apply { readFrom(r as InputStream) })
      RawBodyType.ByteBufferBody ->
        MockResponse()
          .setBody(Buffer().apply { read(r as ByteBuffer) })
    }.addHeader(HeaderNames.ContentType, format.mediaType.toString())
      .apply { headers.headers.forEach { (n, v) -> addHeader(n, v) } }
}
