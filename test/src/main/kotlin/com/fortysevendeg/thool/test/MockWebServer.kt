package com.fortysevendeg.thool.test

import arrow.core.Nullable
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.model.Address
import com.fortysevendeg.thool.model.Body
import com.fortysevendeg.thool.model.ConnectionInfo
import com.fortysevendeg.thool.model.Header
import com.fortysevendeg.thool.model.Method
import com.fortysevendeg.thool.model.QueryParams
import com.fortysevendeg.thool.model.ServerRequest
import com.fortysevendeg.thool.model.ServerResponse
import com.fortysevendeg.thool.model.StatusCode
import com.fortysevendeg.thool.model.Uri
import com.fortysevendeg.thool.server.ServerEndpoint
import com.fortysevendeg.thool.server.interpreter.RequestBody
import com.fortysevendeg.thool.server.interpreter.ServerInterpreter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.nio.ByteBuffer

public fun <I, E, O> ServerEndpoint<I, E, O>.toDispatcher(): Dispatcher =
  object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      val serverRequest = request.toServerRequest()
      val interpreter = ServerInterpreter(
        serverRequest,
        RequestBody(request),
        emptyList()
      )

      return runBlocking(Dispatchers.Default) {
        interpreter.invoke(this@toDispatcher)?.let {
          MockResponse()
            .setResponseCode(it.code.code)
            .setBody(it)
            .apply {
              it.headers.forEach { (name, value) -> addHeader(name, value) }
            }
        } ?: MockResponse().setResponseCode(StatusCode.NotFound.code)
      }
    }
  }

internal class RequestBody(val ctx: RecordedRequest) : RequestBody {
  @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST", "BlockingMethodInNonBlockingContext")
  override suspend fun <R> toRaw(bodyType: EndpointIO.Body<R, *>): R {
    return when (bodyType) {
      is EndpointIO.ByteArrayBody -> withContext(Dispatchers.IO) { ctx.body.readByteArray() }
      is EndpointIO.ByteBufferBody -> withContext(Dispatchers.IO) { ByteBuffer.wrap(ctx.body.readByteArray()) }
      is EndpointIO.InputStreamBody -> ctx.body.inputStream()
      is EndpointIO.StringBody -> withContext(Dispatchers.IO) { ctx.body.readByteArray().toString(bodyType.charset) }
    } as R
  }
}

public fun RecordedRequest.toServerRequest(): ServerRequest =
  ServerRequest(
    protocol = "mocked-protocol",
    connectionInfo = ConnectionInfo(Nullable.zip(requestUrl?.host, requestUrl?.port, ::Address), null, null),
    method = Method(requireNotNull(method) { "No http method specified." }),
    // TODO seems that it can be constructed directly without parsing
    uri = requireNotNull(Uri(requestUrl?.toUri().toString())),
    headers = headers.map { (n, v) -> Header(n, v) },
    pathSegments = requestUrl?.pathSegments.orEmpty(),
    queryParameters = requestUrl?.queryParameterNames
      .orEmpty()
      .map { name -> Pair(name, requestUrl?.queryParameterValues(name)?.filterNotNull().orEmpty()) }
      .toList()
      .let(::QueryParams)
  )

public fun MockResponse.setBody(response: ServerResponse): MockResponse =
  when (val r = response.body) {
    is Body.ByteArray -> setBody(Buffer().apply { write(r.byteArray) })
    is Body.ByteBuffer -> setBody(Buffer().apply { write(r.byteBuffer) })
    is Body.InputStream -> setBody(Buffer().apply { readFrom(r.inputStream) })
    is Body.String -> setBody(r.string)
    else -> this
  }
