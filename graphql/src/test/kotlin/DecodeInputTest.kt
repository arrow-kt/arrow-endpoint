import com.fortysevendeg.thool.Codec
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.Schema
import com.fortysevendeg.thool.Thool
import com.fortysevendeg.thool.input
import com.fortysevendeg.thool.output
import com.fortysevendeg.thool.product
import graphql.schema.DataFetchingEnvironmentImpl
import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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

class DecodeInputTest : StringSpec({

  "Can resolve Query Params" {
    val endpoint = Endpoint
      .get { "hello" / "world" }
      .input(Thool.query("project", Codec.string))
      .input(Thool.query("language", Codec.string))
      .output(Thool.anyJsonBody(Project.jsonCodec))

    val env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
      .arguments(
        mapOf(
          "project" to "test",
          "language" to "Kotlin"
        )
      ).build()

    when (val res = endpoint.input.toInput(env)) {
      is DecodeResult.Value -> res.value.asAny shouldBe Pair("test", "Kotlin")
      else -> fail("Should've been Value")
    }
  }
})
