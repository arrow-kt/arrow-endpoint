import arrow.core.Either
import arrow.core.right
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.server.ServerEndpoint
import io.ktor.application.Application
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.input
import com.fortysevendegrees.thool.ktor.install
import com.fortysevendegrees.thool.output
import com.fortysevendegrees.thool.product
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Project(
  val name: String,
  val language: String = "kotlin"
) {
  companion object {
    val schema: Schema<Project> = Schema.product(
      Project::name to Schema.string,
      Project::language to Schema.string.default("kotlin")
    )

    val jsonCodec = Codec.json(schema, { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }
  }
}

fun Application.endpointModule() = Thool {
  val pong: Endpoint<Unit, Nothing, String> = Endpoint
    .get()
    .input(fixedPath("ping"))
    .output(stringBody())

  install(ServerEndpoint(pong) { Either.Right("Pong") })

  install(ServerEndpoint(Endpoint.get().input(fixedPath("empty"))) { it.right() })

  val helloWorld: Endpoint<Pair<String, String>, Nothing, Project> =
    Endpoint
      .get()
      .input(fixedPath("hello"))
      .input(query("project", Codec.string))
      .input(query("language", Codec.string))
      .output(anyJsonBody(Project.jsonCodec))

  install(
    ServerEndpoint(helloWorld) { (project, language) ->
      Either.Right(Project("other-$project", "other-$language"))
    }
  )
}
