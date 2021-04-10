import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLRequestParser
import com.expediagroup.graphql.server.execution.GraphQLServer
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.Thool.fixedPath
import com.fortysevendegrees.thool.Thool.get
import com.fortysevendegrees.thool.Thool.post
import com.fortysevendegrees.thool.withInput
import com.fortysevendegrees.thool.withOutput
import graphql.GraphQL
import graphql.schema.GraphQLSchema
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import java.io.IOException

val helloWorld: Endpoint<Pair<String, String>, Unit, Project> =
  Thool.endpoint
    .get()
    .withInput(Thool.fixedPath("hello"))
    .withInput(Thool.fixedPath("world"))
    .withInput(Thool.fixedPath("test"))
    .withInput(Thool.query("project", Codec.string))
    .withInput(Thool.query("language", Codec.string))
    .withOutput(Thool.anyJsonBody(Project.jsonCodec))

val post: Endpoint<Pair<String, String>, Unit, Int> =
  Thool.endpoint
    .post()
    .withInput(Thool.fixedPath("test"))
    .withInput(fixedPath("other"))
    .withInput(Thool.query("project", Codec.string))
    .withInput(Thool.query("language", Codec.string))
    .withOutput(
      Thool.plainBody(
        Codec.stringCodec(
          Schema.int
        ) { 1 }
      )
    )

val schema: GraphQLSchema = listOf(
  helloWorld,
  post
).toSchema()

val ql: GraphQL = GraphQL.newGraphQL(schema).build()

private val mapper = jacksonObjectMapper()
private val ktorGraphQLServer = getGraphQLServer(ql, mapper)

fun Application.graphQLModule() {
  routing {
    post("graphql") {
      // Execute the query against the schema
      val result = ktorGraphQLServer.execute(call.request)

      if (result != null) {
        // write response as json
        val json = mapper.writeValueAsString(result)
        call.response.call.respond(json)
      } else {
        call.response.call.respond(HttpStatusCode.BadRequest, "Invalid request")
      }
    }

    get("playground") {
      call.respondText(buildPlaygroundHtml("graphql", "subscriptions"), ContentType.Text.Html)
    }
  }
}

private suspend fun buildPlaygroundHtml(graphQLEndpoint: String, subscriptionsEndpoint: String): String =
  Application::class.java.classLoader.getResource("graphql-playground.html")?.readText()
    ?.replace("\${graphQLEndpoint}", graphQLEndpoint)
    ?.replace("\${subscriptionsEndpoint}", subscriptionsEndpoint)
    ?: throw IllegalStateException("graphql-playground.html cannot be found in the classpath")

data class User(val email: String, val firstName: String, val lastName: String, val universityId: Int)

/**
 * Example of a custom [GraphQLContext]
 */
data class AuthorizedContext(
  val authorizedUser: User? = null,
  var guestUUID: String? = null,
  val customHeader: String? = null
) : GraphQLContext

/**
 * Custom logic for how this example app should create its context given the [ApplicationRequest]
 */
class KtorGraphQLContextFactory : GraphQLContextFactory<AuthorizedContext, ApplicationRequest> {
  override suspend fun generateContext(request: ApplicationRequest): AuthorizedContext {
    val loggedInUser =
      User(email = "fake@site.com", firstName = "Someone", lastName = "You Don't know", universityId = 4)
    // Parse any headers from the Ktor request
    val customHeader: String? = request.headers["my-custom-header"]
    return AuthorizedContext(loggedInUser, customHeader = customHeader)
  }
}

/**
 * Custom logic for how Ktor parses the incoming [ApplicationRequest] into the [GraphQLServerRequest]
 */
class KtorGraphQLRequestParser(private val mapper: ObjectMapper) : GraphQLRequestParser<ApplicationRequest> {
  @Suppress("BlockingMethodInNonBlockingContext")
  override suspend fun parseRequest(request: ApplicationRequest): GraphQLServerRequest = try {
    val rawRequest = request.call.receiveText()
    mapper.readValue(rawRequest, GraphQLServerRequest::class.java)
  } catch (e: IOException) {
    throw IOException("Unable to parse GraphQL payload.")
  }
}

/**
 * Helper method for how this Ktor example creates the common [GraphQLServer] object that
 * can handle requests.
 */
class KtorGraphQLServer(
  requestParser: KtorGraphQLRequestParser,
  contextFactory: KtorGraphQLContextFactory,
  requestHandler: GraphQLRequestHandler
) : GraphQLServer<ApplicationRequest>(requestParser, contextFactory, requestHandler)

fun getGraphQLServer(ql: GraphQL, mapper: ObjectMapper): KtorGraphQLServer {
//  val dataLoaderRegistryFactory = KtorDataLoaderRegistryFactory()
  val requestParser = KtorGraphQLRequestParser(mapper)
  val contextFactory = KtorGraphQLContextFactory()
  val requestHandler = GraphQLRequestHandler(ql/*, dataLoaderRegistryFactory*/)

  return KtorGraphQLServer(requestParser, contextFactory, requestHandler)
}
