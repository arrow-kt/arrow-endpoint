package com.fortysevendegrees.thool.test

import com.fortysevendegrees.thool.ConnectionInfo
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.RawBodyType
import com.fortysevendegrees.thool.ServerRequest
import com.fortysevendegrees.thool.model.Header
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.QueryParams
import com.fortysevendegrees.thool.model.Uri
import com.fortysevendegrees.thool.server.intrepreter.RequestBody
import kotlinx.coroutines.flow.Flow
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.nio.ByteBuffer

fun <I, E, O> Endpoint<I, E, O>.toDispatcher(): Dispatcher =
  object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse =
      TODO()
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

internal typealias KtorResponseBody = MockResponse

internal class KtorServerRequest(val ctx: RecordedRequest) : ServerRequest {
  override val protocol: String = "mocked-protocol"
  override val connectionInfo: ConnectionInfo by lazy { ConnectionInfo(null, null, null) }
  override val underlying: Any = ctx

  override val uri: Uri
    get() = TODO("Uri.unsafeParse(ctx.request.uri.toString())")

  // TODO fix with proper path decoding
  override fun pathSegments(): List<String> =
    ctx.path?.dropWhile { it == '/' }?.split("/").orEmpty()

  override fun queryParameters(): QueryParams = TODO("???")

  override val method: Method =
    Method(requireNotNull(ctx.method) { "No http method specified." })

  override val headers: List<Header> =
    ctx.headers.map { (n, v) -> Header(n, v) }
}
