import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.request.ApplicationRequest

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
    val loggedInUser = User(
      email = "fake@site.com",
      firstName = "Someone",
      lastName = "You Don't know",
      universityId = 4
    )

    // Parse any headers from the Ktor request
    val customHeader: String? = request.headers["my-custom-header"]

    return AuthorizedContext(loggedInUser, customHeader = customHeader)
  }
}