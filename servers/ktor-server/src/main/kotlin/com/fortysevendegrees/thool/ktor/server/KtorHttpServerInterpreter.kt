package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.server.ServerEndpoint
import com.fortysevendegrees.thool.server.intrepreter.ServerInterpreter
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.routing.routing

fun <I, E, O> Application.install(ses: ServerEndpoint<I, E, O>): Routing =
  install(listOf(ses))

fun <I, E, O> Application.install(ses: List<ServerEndpoint<I, E, O>>): Routing =
  routing {
    ses.forEach { endpoint ->
      route(
        endpoint.endpoint.input.path(),
        endpoint.endpoint.input.toHttpMethod()
      ) {
        handle {
          val serverRequest = KtorServerRequest(call)
          val interpreter = ServerInterpreter(
            serverRequest,
            KtorRequestBody(call),
            KtorToResponseBody(),
            emptyList()
          )

          interpreter.invoke(ses)?.let {
            when (it.body) {
              null -> call.respond(HttpStatusCode.fromValue(it.code.code))
              else -> call.respond(HttpStatusCode.fromValue(it.code.code), it.body as KtorResponseBody)
            }
          }
        }
      }
    }
  }

fun EndpointInput<*>.toHttpMethod(): HttpMethod =
  HttpMethod((method() ?: Method.GET).value)

fun EndpointInput<*>.path(): String =
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

fun handleInputPair(
  left: EndpointInput<*>,
  right: EndpointInput<*>,
): String {
  val left = (left as EndpointInput<Any?>).path()
  val right = (right as EndpointInput<Any?>).path()
  return createPath(left, right)
}

fun createPath(left: String, right: String): String =
  when {
    left.isBlank() -> right
    right.isBlank() -> left
    else -> "$left/$right"
  }
