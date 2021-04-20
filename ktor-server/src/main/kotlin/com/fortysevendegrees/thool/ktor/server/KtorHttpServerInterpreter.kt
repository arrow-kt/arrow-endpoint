package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.server.ServerEndpoint
import com.fortysevendegrees.thool.server.intrepreter.ServerInterpreter
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond

fun <I, E, O> Application.install(ses: ServerEndpoint<I, E, O>): Unit =
  install(listOf(ses))

fun <I, E, O> Application.install(ses: List<ServerEndpoint<I, E, O>>): Unit =
  intercept(ApplicationCallPipeline.Call) {
    val serverRequest = KtorServerRequest(call)
    val interpreter = ServerInterpreter(
      serverRequest,
      KtorRequestBody(call),
      KtorToResponseBody(),
      emptyList()
    )

    interpreter.invoke(ses)?.let {
      println(it)
      when (it.body) {
        null -> call.respond(HttpStatusCode.fromValue(it.code.code))
        else -> call.respond(HttpStatusCode.fromValue(it.code.code), it.body as KtorResponseBody)
      }
    }
  }
