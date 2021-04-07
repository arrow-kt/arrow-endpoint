import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLServer
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.GraphQL
import io.ktor.request.ApplicationRequest

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