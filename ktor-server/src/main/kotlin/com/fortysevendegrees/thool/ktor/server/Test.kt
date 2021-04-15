package com.fortysevendegrees.thool.ktor.server

import arrow.core.Either
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.input
import com.fortysevendegrees.thool.output
import com.fortysevendegrees.thool.server.ServerEndpoint
import io.ktor.application.Application

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) = Thool {
  val pong: Endpoint<Unit, Nothing, String> =
    Endpoint
      .get("ping")
      .output(stringBody())

  install(
    ServerEndpoint(pong) {
      Either.Right("Pong")
    }
  )

  val helloWorld: Endpoint<Pair<String, String>, Nothing, Project> =
    Endpoint
      .get("hello")
      .input(query("project", Codec.string))
      .input(query("language", Codec.string))
      .output(anyJsonBody(Project.jsonCodec))

  println(helloWorld.renderPath()) // /hello?project={project}&language={language}

  install(
    ServerEndpoint(helloWorld) { (project, language) ->
      Either.Right(Project("other-$project", "other-$language"))
    }
  )
}
