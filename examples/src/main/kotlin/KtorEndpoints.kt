import arrow.core.Either
import arrow.core.right
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.withInput
import com.fortysevendegrees.thool.withOutput
import com.fortysevendegrees.thool.server.ServerEndpoint
import io.ktor.application.Application
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.FieldName
import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.SchemaType.SObject.SProduct
import com.fortysevendegrees.thool.SchemaType.SObjectInfo
import com.fortysevendegrees.thool.ktor.install
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
    val schema: Schema<Project> = Schema(
      SProduct(
        SObjectInfo("Project"),
        listOf(
          Pair(
            FieldName("name"),
            Schema.string.description("The name of this project")
          ),
          Pair(
            FieldName("language"),
            Schema.string.description("The programming language used for this project").default("kotlin")
          )
        )
      )
    )

    val jsonCodec = Codec.json(schema, { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }
  }
}

fun Application.endpointModule() = Thool {
  val pong: Endpoint<Unit, Unit, String> = endpoint
    .get()
    .withInput(fixedPath("ping"))
    .withOutput(stringBody())

  install(ServerEndpoint(pong) { Either.Right("Pong") })

  install(ServerEndpoint(endpoint.get().withInput(fixedPath("empty"))) { it.right() })

  val helloWorld: Endpoint<Pair<String, String>, Unit, Project> =
    endpoint
      .get()
      .withInput(fixedPath("hello"))
      .withInput(query("project", Codec.string))
      .withInput(query("language", Codec.string))
      .withOutput(anyJsonBody(Project.jsonCodec))

  install(
    ServerEndpoint(helloWorld) { (project, language) ->
      Either.Right(Project("other-$project", "other-$language"))
    }
  )
}
