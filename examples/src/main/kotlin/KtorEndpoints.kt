import arrow.core.Either
import arrow.core.right
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.server.ServerEndpoint
import io.ktor.application.Application
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.docs.openapi.toOpenAPI
import com.fortysevendegrees.thool.input
import com.fortysevendegrees.thool.ktor.server.install
import com.fortysevendegrees.thool.model.CodecFormat
import com.fortysevendegrees.thool.output
import com.fortysevendegrees.thool.product
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
public data class Project(
  val name: String,
  val language: String
) {
  public companion object {
    public val schema: Schema<Project> = Schema.product(
      Project::name to Schema.string,
      Project::language to Schema.string.default("kotlin")
    )

    public val jsonCodec: Codec<String, Project, CodecFormat.Json> =
      Codec.json(schema, { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }
  }
}

public val pong: Endpoint<Unit, Unit, String> = Endpoint
  .get()
  .input(Thool.fixedPath("ping"))
  .output(Thool.stringBody())

public val openApiServerEndpoint: ServerEndpoint<Unit, Unit, String> =
  Endpoint
    .get("openapi")
    .output(Thool.stringBody())
    .logic {
      listOf(
        helloWorld,
        pong
      )
        .toOpenAPI("Example Server", "0.0.1")
        .toJson()
        .right()
    }

public fun Application.endpointModule() = Thool {
  install(ServerEndpoint(pong) { Either.Right("Pong") })

  install(ServerEndpoint(Endpoint.get().input(fixedPath("empty"))) { it.right() })

  install(
    ServerEndpoint(helloWorld) { (project, language) ->
      Either.Right(Project("other-$project", "other-$language"))
    }
  )

  install(openApiServerEndpoint)
}
