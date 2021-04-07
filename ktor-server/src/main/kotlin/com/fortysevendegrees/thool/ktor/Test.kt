package com.fortysevendegrees.thool.ktor

import arrow.core.Either
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.withInput
import com.fortysevendegrees.thool.withOutput
import com.fortysevendegrees.thool.server.ServerEndpoint
import io.ktor.application.Application

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) = Thool {
  val pong: Endpoint<Unit, Unit, String> = endpoint
    .get()
    .withInput(fixedPath("ping"))
    .withOutput(stringBody())

  install(
    ServerEndpoint(pong) {
      Either.Right("Pong")
    }
  )

  val helloWorld: Endpoint<Pair<String, String>, Unit, Project> =
    endpoint
      .get()
      .withInput(fixedPath("hello"))
      .withInput(query("project", Codec.string))
      .withInput(query("language", Codec.string))
      .withOutput(anyJsonBody(Project.jsonCodec))

  println(helloWorld.renderPath()) // /hello?project={project}&language={language}

  install(
    ServerEndpoint(helloWorld) { (project, language) ->
      Either.Right(Project("other-$project", "other-$language"))
    }
  )
}
