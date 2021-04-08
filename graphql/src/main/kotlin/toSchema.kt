import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.EndpointOutput
import com.fortysevendegrees.thool.EndpointTransput
import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.SchemaType
import com.fortysevendegrees.thool.model.Method
import graphql.Scalars
import graphql.schema.Coercing
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import java.lang.IllegalStateException

/**
 * The conversion rules are the following:
 *   - GET endpoints are turned into Queries
 *   - PUT, POST and DELETE endpoints are turned into Mutations
 *   - fixed query paths are used to name GraphQL fields (e.g. an endpoint /book/add will give a GraphQL field named bookAdd)
 *   - query parameters, headers, cookies and request body are used as GraphQL arguments
 */
fun <I, E, O> Endpoint<I, E, O>.toSchema(): GraphQLSchema {
  val builder = GraphQLSchema.Builder()
    .description(info.description)

  // Not covering all methods here yet :scream:
  when (input.method() ?: Method.GET) {
    Method.PUT, Method.POST, Method.DELETE -> TODO("Mutation")
    Method.GET -> builder.query(
      GraphQLObjectType.Builder()
        .name("Query")
        .description(info.description)
        .field(generateFunction())
        .build()
    )
    else -> TODO("RIP")
  }

  return builder.build()
}

fun <I, E, O> Endpoint<I, E, O>.generateFunction(): GraphQLFieldDefinition =
  GraphQLFieldDefinition.Builder()
    .name(extractPath())
    .apply { if (info.deprecated) deprecate("This method is deprecated") }
    .arguments(input.getArguments())
    .type(output.getReturnType())
    .build()

fun <O> EndpointOutput<O>.getReturnType(): GraphQLOutputType =
  when (this) {
    is EndpointOutput.StatusCode -> Scalars.GraphQLString
    is EndpointIO.Body<*, *> ->
      GraphQLObjectType.Builder()
        .description(info.description)
        .name("body")
//        .type(codec.schema().toType())
        .build()
    is EndpointIO.Empty ->
      GraphQLScalarType.newScalar()
        .name("Empty")
        .description("Represents empty Input or Output")
        .coercing(object : Coercing<Unit, Unit> {
          override fun serialize(dataFetcherResult: Any?) = Unit
          override fun parseValue(input: Any?) = Unit
          override fun parseLiteral(input: Any?) = Unit
        })
        .build()
    is EndpointOutput.Void ->
      GraphQLScalarType.newScalar()
        .name("Void")
        .description("Represents no error output")
        .coercing(object : Coercing<Unit, Unit> {
          override fun serialize(dataFetcherResult: Any?) = Unit
          override fun parseValue(input: Any?) = Unit
          override fun parseLiteral(input: Any?) = Unit
        })
        .build()
    is EndpointIO.Header -> TODO()
    is EndpointIO.StreamBody -> TODO()
    is EndpointOutput.FixedStatusCode -> TODO()

    // What to do with tupled outputs ??
    is EndpointIO.MappedPair<*, *, *, *> -> TODO("MapperPair output: $this")
    is EndpointOutput.Pair<*, *, *> -> TODO("Tupled output: $this")
    is EndpointOutput.MappedPair<*, *, *, *> -> TODO("MapperPair output: $this")
    is EndpointIO.Pair<*, *, *> -> TODO("Tupled output: $this")
  }

fun <I> EndpointInput<I>.getArguments(): List<GraphQLArgument> =
  traverseInputs(
    { it !is EndpointTransput.Pair<*> && it !is EndpointInput.MappedPair<*, *, *, *> && it !is EndpointOutput.MappedPair<*, *, *, *> && it !is EndpointIO.MappedPair<*, *, *, *> }
  ) {
    when (it) {
      is EndpointIO.Body<*, *> -> listOf(
        GraphQLArgument.Builder()
          .description(it.info.description)
          .name("body")
          .type(it.codec.schema().toType())
          .build()
      )
      is EndpointInput.Query -> listOf(
        GraphQLArgument.Builder()
          .name(it.name)
          .description(it.info.description)
          .type(it.codec.schema().toType())
          .build()
      )

      is EndpointInput.Cookie -> listOf(
        GraphQLArgument.Builder()
          .name(it.name)
          .description(it.info.description)
          .type(it.codec.schema().toType())
          .build()
      )

      is EndpointInput.PathCapture -> listOf(
        GraphQLArgument.Builder()
          .name(it.name)
          .description(it.info.description)
          .type(it.codec.schema().toType())
          .build()
      )

      is EndpointIO.Empty -> emptyList()
      is EndpointInput.FixedMethod -> emptyList()
      is EndpointInput.FixedPath -> emptyList()

      is EndpointInput.PathsCapture -> TODO()
      is EndpointIO.Header -> TODO()
      is EndpointIO.StreamBody -> TODO()
      is EndpointInput.QueryParams -> TODO()
      else -> TODO("pair or mapped pair should be impossible: $it")
    }
  }

fun <A> Schema<A>.toType(): GraphQLInputType =
  when (val type = this.schemaType) {
    SchemaType.SBoolean -> Scalars.GraphQLBoolean
    SchemaType.SInteger -> Scalars.GraphQLInt
    SchemaType.SNumber -> Scalars.GraphQLFloat
    SchemaType.SString -> Scalars.GraphQLString
    SchemaType.SDate -> Scalars.GraphQLString
    SchemaType.SDateTime -> Scalars.GraphQLString
    SchemaType.SBinary -> Scalars.GraphQLString
    is SchemaType.SArray -> GraphQLList.list(type.element.toType())
    is SchemaType.SObject.SProduct ->
      GraphQLInputObjectType.newInputObject()
        .name(type.info.fullName)
        .fields(
          type.fields.map { (name, schema) ->
            GraphQLInputObjectField.newInputObjectField()
              .name(name.name)
              .type(schema.toType())
//            .description()
              .build()
          }
        )
//    .description()
        .build()

    is SchemaType.SObject.SOpenProduct -> TODO()
    is SchemaType.SObject.SCoproduct -> TODO("???")
    is Schema.SCoproduct -> TODO("???")
    is SchemaType.SRef -> TODO()
  }

private fun <I, E, O> Endpoint<I, E, O>.extractPath(): String {
  val endpointName = info.name?.let(::replaceIllegalChars)
  val path = input.asListOfBasicInputs(includeAuth = false)
    .mapNotNull { (it as? EndpointInput.FixedPath)?.s }
    .reduceOrNull { acc: String, s: String ->
      acc + s.capitalize()
    } ?: "root"

  return endpointName ?: path
}

private fun replaceIllegalChars(s: String): String =
  s.replace("\\W+", "_")

private fun <I> extractArgNames(input: EndpointInput<I>): Map<String, Pair<String, String?>?> =
  input.traverseInputs(::matches) { i ->
    when {
      i is EndpointInput.PathCapture && i.name != null -> listOf(Pair(i.name!!, i.info.description))
      i is EndpointInput.Query -> listOf(Pair(i.name, i.info.description))
      i is EndpointInput.Cookie -> listOf(Pair(i.name, i.info.description))
      i is EndpointIO.Header -> listOf(Pair(i.name, i.info.description))
      i is EndpointIO.Body<*, *> -> listOf(Pair("body", i.info.description))
      i is EndpointInput.MappedPair<*, *, *, *> -> listOf(null)
      i is EndpointIO.MappedPair<*, *, *, *> -> listOf(null)
      else -> throw IllegalStateException("Called but didn't match. Internal error.")
    }
  }.mapIndexed { index, pair ->
    Pair("_${index + 1}", pair?.let { (name, desc) -> Pair(name.replace("-", "_"), desc) })
  }.toMap()

private fun matches(input: EndpointInput<*>): Boolean =
  input is EndpointInput.PathCapture && input.name != null || input is EndpointInput.Query ||
    input is EndpointInput.Cookie || input is EndpointIO.Header ||
    input is EndpointIO.Body<*, *> || input is EndpointInput.MappedPair<*, *, *, *> ||
    input is EndpointIO.MappedPair<*, *, *, *>
