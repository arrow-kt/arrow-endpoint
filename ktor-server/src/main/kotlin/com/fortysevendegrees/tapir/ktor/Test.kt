package com.fortysevendegrees.tapir.ktor

import arrow.core.Either
import com.fortysevendegrees.tapir.Codec
import com.fortysevendegrees.tapir.Endpoint
import com.fortysevendegrees.tapir.EndpointIO
import com.fortysevendegrees.tapir.Tapir
import com.fortysevendegrees.tapir.withInput
import com.fortysevendegrees.tapir.withOutput
import com.fortysevendegrees.tapir.server.ServerEndpoint
import io.ktor.application.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) = Tapir {
  val pong: Endpoint<Unit, Unit, String> = endpoint
    .get()
    .withInput(fixedPath("ping"))
    .withOutput(stringBody())

  install(ServerEndpoint(pong) {
    Either.Right("Pong")
  })

  val helloWorld: Endpoint<Pair<String, String>, Unit, Project> =
    endpoint
      .get()
      .withInput(fixedPath("hello"))
      .withInput(query("project", Codec.string))
      .withInput(query("language", Codec.string))
      .withOutput(anyJsonBody(Project.jsonCodec))

  println(helloWorld.renderPath()) // /hello?project={project}&language={language}

  install(ServerEndpoint(helloWorld) { (project, language) ->
    Either.Right(Project("other-$project", "other-$language"))
  })
}
