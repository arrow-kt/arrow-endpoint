package arrow.endpoint.test

import arrow.core.Nullable
import arrow.endpoint.EndpointIO
import arrow.endpoint.model.Address
import arrow.endpoint.model.Body
import arrow.endpoint.model.ConnectionInfo
import arrow.endpoint.model.Header
import arrow.endpoint.model.Method
import arrow.endpoint.model.QueryParams
import arrow.endpoint.model.ServerRequest
import arrow.endpoint.model.ServerResponse
import arrow.endpoint.model.StatusCode
import arrow.endpoint.model.Uri
import arrow.endpoint.server.ServerEndpoint
import arrow.endpoint.server.interpreter.RequestBody
import arrow.endpoint.server.interpreter.ServerInterpreter
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
    is Body.String -> setBody(r.string)
    else -> this
  }
