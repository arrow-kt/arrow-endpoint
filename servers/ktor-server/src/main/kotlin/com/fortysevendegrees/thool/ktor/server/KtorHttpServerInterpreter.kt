package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.model.Body
import com.fortysevendegrees.thool.model.Header
import com.fortysevendegrees.thool.model.ServerResponse
import com.fortysevendegrees.thool.model.header
import com.fortysevendegrees.thool.server.ServerEndpoint
import com.fortysevendegrees.thool.server.interpreter.ServerInterpreter
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.OutputStreamContent
import io.ktor.http.content.TextContent
import io.ktor.response.header
import io.ktor.response.respond

public fun <I, E, O> Application.install(ses: ServerEndpoint<I, E, O>): Unit =
  install(listOf(ses))

public fun <I, E, O> Application.install(ses: List<ServerEndpoint<I, E, O>>): Unit =
  intercept(ApplicationCallPipeline.ApplicationPhase.Call) {
    val interpreter = ServerInterpreter(
      call.toServerRequest(),
      KtorRequestBody(call),
      emptyList()
    )

    interpreter.invoke(ses)?.let {
      it.headers.forEach { (name, value) ->
        // Header(s) Content-Type are controlled by the engine and cannot be set explicitly
        if (name != Header.ContentType) call.response.header(name, value)
      }

      when (val body = it.body()) {
        null -> call.respond(HttpStatusCode.fromValue(it.code.code))
        else -> call.respond(HttpStatusCode.fromValue(it.code.code), body)
      }
    }
  }

fun ServerResponse.body(): OutgoingContent? =
  when (val body = body) {
    is Body.ByteArray -> ByteArrayContent(
      body.toByteArray(),
      headers.header(Header.ContentType)?.let(ContentType::parse),
      HttpStatusCode.fromValue(code.code)
    )
    is Body.ByteBuffer -> ByteArrayContent(
      body.toByteArray(),
      headers.header(Header.ContentType)?.let(ContentType::parse),
      HttpStatusCode.fromValue(code.code)
    )
    is Body.String -> TextContent(
      body.string,
      // TODO work this into Body.String ADT
      requireNotNull(headers.header(Header.ContentType)?.let(ContentType::parse)),
      HttpStatusCode.fromValue(code.code)
    )
    is Body.InputStream -> OutputStreamContent(
      {
        body.inputStream.copyTo(this)
      },
      // TODO work this into Body.InputStream ADT
      requireNotNull(headers.header(Header.ContentType)?.let(ContentType::parse)),
      HttpStatusCode.fromValue(code.code)
    )
    else -> null
  }
