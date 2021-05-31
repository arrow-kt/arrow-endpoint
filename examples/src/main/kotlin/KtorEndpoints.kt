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

    public val jsonCodec: JsonCodec<Project> =
      Codec.json(schema, { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }
  }
}

public val pong: Endpoint<Unit, Unit, String> = Endpoint
  .get()
  .input(fixedPath("ping"))
  .output(stringBody())

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
