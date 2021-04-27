package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.server.ServerEndpoint
import com.fortysevendegrees.thool.server.interpreter.ServerInterpreter
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.routing.routing

fun <I, E, O> Application.install(ses: ServerEndpoint<I, E, O>): Routing =
  install(listOf(ses))

fun <I, E, O> Application.install(ses: List<ServerEndpoint<I, E, O>>): Routing =
  routing {
    ses.forEach { endpoint ->
      val method = endpoint.endpoint.input.toHttpMethod()
      if (method == null) route(endpoint.endpoint.input.path()) { resolve(listOf(endpoint)) }
      else route(endpoint.endpoint.input.path(), method) { resolve(listOf(endpoint)) }
    }
  }

private fun <I, E, O> Route.resolve(
  ses: List<ServerEndpoint<I, E, O>>
): Unit = handle {
  val serverRequest = KtorServerRequest(this.call)
  val interpreter = ServerInterpreter(
    serverRequest,
    KtorRequestBody(call),
    KtorToResponseBody(),
    emptyList()
  )

  interpreter.invoke(ses)?.let {
    println("====>>> Going to respond: $it")
    when (it.body) {
      null -> call.respond(HttpStatusCode.fromValue(it.code.code))
      else -> call.respond(HttpStatusCode.fromValue(it.code.code), it.body as KtorResponseBody)
    }
  } ?: println("====>>> I am always called with nothing :''''(((")
}

private fun EndpointInput<*>.toHttpMethod(): HttpMethod? =
  method()?.value?.let(::HttpMethod)

private fun EndpointInput<*>.path(): String =
  when (this) {
    is EndpointInput.FixedPath -> s
    // TODO empty path capture == wildcard ?
    is EndpointInput.PathCapture -> name?.let { "{$it}" } ?: TODO("path-capture without name ???")
    is EndpointInput.PathsCapture -> "{...}"

    // These don't influence baseUrl
    is EndpointIO.Body<*, *> -> ""
    is EndpointIO.Empty -> ""
    is EndpointInput.FixedMethod -> ""
    is EndpointIO.Header -> ""
    is EndpointIO.StreamBody -> ""
    is EndpointInput.Query -> ""
    is EndpointInput.Cookie -> ""
    is EndpointInput.QueryParams -> ""

    // Recurse on composition of inputs.
    is EndpointInput.Pair<*, *, *> -> handleInputPair(this.first, this.second)
    is EndpointIO.Pair<*, *, *> -> handleInputPair(this.first, this.second)
    is EndpointIO.MappedPair<*, *, *, *> -> handleInputPair(this.wrapped.first, this.wrapped.second)
    is EndpointInput.MappedPair<*, *, *, *> -> handleInputPair(this.input.first, this.input.second)
  }

private fun handleInputPair(
  left: EndpointInput<*>,
  right: EndpointInput<*>,
): String {
  val left = (left as EndpointInput<Any?>).path()
  val right = (right as EndpointInput<Any?>).path()
  return createPath(left, right)
}

private fun createPath(left: String, right: String): String =
  when {
    left.isBlank() -> right
    right.isBlank() -> left
    else -> "$left/$right"
  }
