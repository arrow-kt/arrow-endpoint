import arrow.core.Either
import arrow.core.right
import com.fortysevendeg.thool.Codec
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.Thool
import com.fortysevendeg.thool.server.ServerEndpoint
import io.ktor.application.Application
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.JsonCodec
import com.fortysevendeg.thool.Schema
import com.fortysevendeg.thool.Thool.fixedPath
import com.fortysevendeg.thool.Thool.stringBody
import com.fortysevendeg.thool.docs.openapi.toOpenAPI
import com.fortysevendeg.thool.input
import com.fortysevendeg.thool.ktor.server.install
import com.fortysevendeg.thool.output
import com.fortysevendeg.thool.product
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
public data class Project(
  val name: String,
  val language: String = "kotlin"
) {
  public companion object {
    public val schema: Schema<Project> = Schema.product(
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
  Endpoint
    .get()
    .input(fixedPath("project"))
    .input(fixedPath("json"))
    .input(
      Thool.query("project", Codec.string)
        .description("The name of the project")
        .example("Arrow Fx Coroutines")
    )
    .input(
      Thool.query("language", Codec.string)
        .description("The primary programming language of the project")
        .default("kotlin")
        .example("java")
    )
    .output(
      Thool.anyJsonBody(Project.jsonCodec)
        .description("The project transformed into json format")
        .default(Project("", "Kotlin"))
        .example(Project("Arrow Fx Coroutines", "Kotlin"))
    )

public val pong: Endpoint<Unit, Unit, String> = Endpoint
  .get()
  .input(fixedPath("ping"))
  .output(stringBody())

private val docs = listOf(helloWorld, pong)
  .toOpenAPI("Example Server", "0.0.1")
  .toJson()

public val openApiServerEndpoint: ServerEndpoint<Unit, Unit, String> =
  Endpoint
    .get("openapi")
    .output(stringBody())
    .logic { docs.right() }

public fun Application.endpointModule(): Unit = Thool {
  install(ServerEndpoint(pong) { Either.Right("Pong") })

  install(ServerEndpoint(Endpoint.get().input(fixedPath("empty"))) { it.right() })

  install(
    ServerEndpoint(helloWorld) { (project, language) ->
      Either.Right(Project("other-$project", "other-$language"))
    }
  )

  install(openApiServerEndpoint)
}
