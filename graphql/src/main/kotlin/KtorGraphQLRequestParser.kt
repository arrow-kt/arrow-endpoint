import com.expediagroup.graphql.server.execution.GraphQLRequestParser
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.request.ApplicationRequest
import io.ktor.request.receiveText
import java.io.IOException

/**
 * Custom logic for how Ktor parses the incoming [ApplicationRequest] into the [GraphQLServerRequest]
 */
class KtorGraphQLRequestParser(
  private val mapper: ObjectMapper
) : GraphQLRequestParser<ApplicationRequest> {

  @Suppress("BlockingMethodInNonBlockingContext")
  override suspend fun parseRequest(request: ApplicationRequest): GraphQLServerRequest = try {
    val rawRequest = request.call.receiveText()
    mapper.readValue(rawRequest, GraphQLServerRequest::class.java)
  } catch (e: IOException) {
    throw IOException("Unable to parse GraphQL payload.")
  }
}