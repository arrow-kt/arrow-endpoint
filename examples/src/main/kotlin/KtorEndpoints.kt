import arrow.core.Either
import arrow.core.right
import arrow.endpoint.ArrowEndpoint
import arrow.endpoint.ArrowEndpoint.fixedPath
import arrow.endpoint.ArrowEndpoint.stringBody
import arrow.endpoint.Codec
import arrow.endpoint.DecodeResult
import arrow.endpoint.Endpoint
import arrow.endpoint.JsonCodec
import arrow.endpoint.Schema
import arrow.endpoint.docs.openapi.toOpenAPI
import arrow.endpoint.input
import arrow.endpoint.ktor.server.install
import arrow.endpoint.output
import arrow.endpoint.product
import arrow.endpoint.server.ServerEndpoint
import io.ktor.application.Application
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
public data class Project(val name: String, val language: String = "kotlin") {
  public companion object {
    public val schema: Schema<Project> =
      Schema.product(
        Project::name to Schema.string,
        Project::language to Schema.string.default("kotlin")
      )

    public val jsonCodec: JsonCodec<Project> = Codec.kotlinxJson(schema)
  }
}

@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified A> Codec.Companion.kotlinxJson(schema: Schema<A>): JsonCodec<A> =
  json(schema, { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }

public val helloWorld: Endpoint<Pair<String, String>, Unit, Project> =
  Endpoint.get()
    .input(fixedPath("project"))
    .input(fixedPath("json"))
    .input(
      ArrowEndpoint.query("project", Codec.string)
        .description("The name of the project")
        .example("Arrow Fx Coroutines")
    )
    .input(
      ArrowEndpoint.query("language", Codec.string)
        .description("The primary programming language of the project")
        .default("kotlin")
        .example("java")
    )
    .output(
      ArrowEndpoint.anyJsonBody(Project.jsonCodec)
        .description("The project transformed into json format")
        .default(Project("", "Kotlin"))
        .example(Project("Arrow Fx Coroutines", "Kotlin"))
    )

public val pong: Endpoint<Unit, Unit, String> =
  Endpoint.get().input(fixedPath("ping")).output(stringBody())

private val docs = listOf(helloWorld, pong).toOpenAPI("Example Server", "0.0.1").toJson()

public val openApiServerEndpoint: ServerEndpoint<Unit, Unit, String> =
  Endpoint.get("openapi").output(stringBody()).logic { docs.right() }

public fun Application.endpointModule(): Unit = ArrowEndpoint {
  install(ServerEndpoint(pong) { Either.Right("Pong") })

  install(ServerEndpoint(Endpoint.get().input(fixedPath("empty"))) { it.right() })

  install(
    ServerEndpoint(helloWorld) { (project, language) ->
      Either.Right(Project("other-$project", "other-$language"))
    }
  )

  install(openApiServerEndpoint)
}
