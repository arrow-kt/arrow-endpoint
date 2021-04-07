import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.Thool.get
import com.fortysevendegrees.thool.withInput
import graphql.GraphQL
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.graphQLModule() {
  install(Routing)

  routing {
    post("graphql") {
      handle(call)
    }

    get("playground") {
      call.respondText(buildPlaygroundHtml("graphql", "subscriptions"), ContentType.Text.Html)
    }
  }
}

val helloWorld =
  Thool.endpoint
    .get()
    .withInput(Thool.fixedPath("hello"))
    .withInput(Thool.fixedPath("world"))
    .withInput(Thool.fixedPath("test"))
    .withInput(Thool.query("project", Codec.string))
    .withInput(Thool.query("language", Codec.string))
//      .withOutput(Thool.anyJsonBody(Codec.json(Schema.string, { DecodeResult.Value(it) }, { it })))

val schema = helloWorld.toSchema(SchemaGeneratorConfig(listOf()))
val ql: GraphQL = GraphQL.newGraphQL(schema).build()

private val mapper = jacksonObjectMapper()
private val ktorGraphQLServer = getGraphQLServer(ql, mapper)

/**
 * Handle incoming Ktor Http requests and send them back to the response methods.
 */
suspend fun handle(applicationCall: ApplicationCall) {
  // Execute the query against the schema
  val result = ktorGraphQLServer.execute(applicationCall.request)

  if (result != null) {
    // write response as json
    val json = mapper.writeValueAsString(result)
    applicationCall.response.call.respond(json)
  } else {
    applicationCall.response.call.respond(HttpStatusCode.BadRequest, "Invalid request")
  }
}

private suspend fun buildPlaygroundHtml(graphQLEndpoint: String, subscriptionsEndpoint: String) =
  Application::class.java.classLoader.getResource("graphql-playground.html")?.readText()
    ?.replace("\${graphQLEndpoint}", graphQLEndpoint)
    ?.replace("\${subscriptionsEndpoint}", subscriptionsEndpoint)
    ?: throw IllegalStateException("graphql-playground.html cannot be found in the classpath")