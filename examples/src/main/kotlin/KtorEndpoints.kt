import arrow.core.Either
import arrow.core.right
import com.fortysevendeg.thool.Codec
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.Thool
import com.fortysevendeg.thool.server.ServerEndpoint
import io.ktor.application.Application
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Schema
import com.fortysevendeg.thool.input
import com.fortysevendeg.thool.ktor.server.install
import com.fortysevendeg.thool.output
import com.fortysevendeg.thool.product
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
    val schema: Schema<Project> = Schema.product(
      Project::name to Schema.string,
      Project::language to Schema.string.default("kotlin")
    )

    val jsonCodec = Codec.json(schema, { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }
  }
}

fun Application.endpointModule() = Thool {
  val pong: Endpoint<Unit, Unit, String> = Endpoint
    .get()
    .input(fixedPath("ping"))
    .output(stringBody())

  install(ServerEndpoint(pong) { Either.Right("Pong") })

  install(ServerEndpoint(Endpoint.get().input(fixedPath("empty"))) { it.right() })

  val helloWorld: Endpoint<Pair<String, String>, Unit, Project> =
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
