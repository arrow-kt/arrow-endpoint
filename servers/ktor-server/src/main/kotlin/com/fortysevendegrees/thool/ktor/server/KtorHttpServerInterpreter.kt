package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.model.Headers
import com.fortysevendegrees.thool.server.ServerEndpoint
import com.fortysevendegrees.thool.server.interpreter.ServerInterpreter
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respond

public fun <I, E, O> Application.install(ses: ServerEndpoint<I, E, O>): Unit =
  install(listOf(ses))

public fun <I, E, O> Application.install(ses: List<ServerEndpoint<I, E, O>>): Unit =
  intercept(ApplicationCallPipeline.ApplicationPhase.Call) {
    val serverRequest = KtorServerRequest(this.call)
    val interpreter = ServerInterpreter(
      serverRequest,
      KtorRequestBody(call),
      KtorToResponseBody(),
      emptyList()
    )

    interpreter.invoke(ses)?.let {
      it.headers.forEach { (name, value) ->
        // Header(s) Content-Type are controlled by the engine and cannot be set explicitly
        if (name != Headers.ContentType) call.response.header(name, value)
      }
      when (it.body) {
        null -> call.respond(HttpStatusCode.fromValue(it.code.code))
        else -> call.respond(HttpStatusCode.fromValue(it.code.code), it.body as KtorResponseBody)
      }
    }
  }
