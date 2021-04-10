import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.EndpointOutput
import com.fortysevendegrees.thool.EndpointTransput
import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.SchemaType
import com.fortysevendegrees.thool.asListOfBasicInputs
import com.fortysevendegrees.thool.method
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.reduce
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
fun List<Endpoint<*, Unit, *>>.toSchema(): GraphQLSchema {
  val builder = GraphQLSchema.Builder()
    .description(mapNotNull { it.info.description }.joinToString(prefix = "- ", separator = "\n- "))

  forEach {
    println(it.info.name)

    // Not covering all methods here yet :scream:
    when (it.input.method() ?: Method.GET) {
      Method.PUT, Method.POST, Method.DELETE -> builder.mutation(
        GraphQLObjectType.Builder()
          .name("Mutation")
          .description(it.info.description)
          .field(it.generateFunction())
          .build()
      )
      Method.GET -> builder.query(
        GraphQLObjectType.Builder()
          .name("Query")
          .description(it.info.description)
          .field(it.generateFunction())
          .build()
      )
      else -> TODO("RIP")
    }
  }

  return builder.build()
}

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
    Method.PUT, Method.POST, Method.DELETE ->
      GraphQLObjectType.Builder()
        .name("Mutation")
        .description(info.description)
        .field(generateFunction())
        .build()
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
    .type(output.getReturnType().firstOrNull() ?: unitScalar)
    .build()

fun <O> EndpointOutput<O>.getReturnType(): List<GraphQLOutputType> =
  reduce(
    ifBody = {
      it.codec.schema().toScalarOrNull()?.let(::listOf) ?: listOf(
        GraphQLObjectType.Builder()
          .description(it.info.description)
          .name("body: ${it.codec.schema().name() ?: ""}")
          .fields(it.codec.schema().toFields())
          .build()
      )
    },
    ifStatusCode = {
      it.codec.schema().toScalarOrNull()?.let(::listOf) ?: listOf(
        GraphQLObjectType.Builder()
          .description(it.info.description)
          .name(it.codec.schema().name() ?: "")
          .fields(it.codec.schema().toFields())
          .build()
      )
    },
    ifFixedStatuscode = {
      it.codec.schema().toScalarOrNull()?.let(::listOf) ?: listOf(
        GraphQLObjectType.Builder()
          .description(it.info.description)
          .name(it.codec.schema().name() ?: "")
          .fields(it.codec.schema().toFields())
          .build()
      )
    },
    ifVoid = { emptyList() },
    ifEmpty = { emptyList() },
    ifHeader = {
      it.codec.schema().toScalarOrNull()?.let(::listOf) ?: listOf(
        GraphQLObjectType.Builder()
          .description(it.info.description)
          .name(it.codec.schema().name() ?: "")
          .fields(it.codec.schema().toFields())
          .build()
      )
    },
    ifStreamBody = { TODO("Subscription???") }
  )

fun Schema<*>.toScalarOrNull(): GraphQLScalarType? =
  when (this.schemaType) {
    SchemaType.SBoolean -> Scalars.GraphQLBoolean
    SchemaType.SInteger -> Scalars.GraphQLInt
    SchemaType.SNumber -> Scalars.GraphQLFloat
    SchemaType.SString -> Scalars.GraphQLString
    SchemaType.SDate -> Scalars.GraphQLString
    SchemaType.SDateTime -> Scalars.GraphQLString
    SchemaType.SBinary -> Scalars.GraphQLString
    is SchemaType.SArray -> null
    is SchemaType.SObject.SCoproduct -> null
    is SchemaType.SObject.SOpenProduct -> null
    is SchemaType.SObject.SProduct -> null
    is SchemaType.SRef -> null
  }

val unitScalar: GraphQLScalarType =
  GraphQLScalarType.newScalar()
    .name("Unit")
    .description("The Unit type has exactly one value, and is used when there is no other meaningful value that could be returned.")
    .coercing(object : Coercing<Any?, Any?> {
      override fun serialize(dataFetcherResult: Any?): Any? = Unit
      override fun parseValue(input: Any?): Any? = Unit
      override fun parseLiteral(input: Any?): Any? = Unit
    }).build()

val voidScalar: GraphQLScalarType =
  GraphQLScalarType.newScalar()
    .name("Void")
    .coercing(object : Coercing<Any?, Any?> {
      override fun serialize(dataFetcherResult: Any?): Any? = Unit
      override fun parseValue(input: Any?): Any? = Unit
      override fun parseLiteral(input: Any?): Any? = Unit
    }).build()

fun <I> EndpointInput<I>.getArguments(): List<GraphQLArgument> =
  reduce(
    ifBody = {
      listOf(
        GraphQLArgument.Builder()
          .description(it.info.description)
          .name(it.codec.schema().name() ?: "body")
          .type(it.codec.schema().toInputType())
          .build()
      )
    },
    ifQuery = {
      listOf(
        GraphQLArgument.Builder()
          .name(it.name)
          .description(it.info.description)
          .type(it.codec.schema().toInputType())
          .build()
      )
    },
    ifCookie = {
      listOf(
        GraphQLArgument.Builder()
          .name(it.name)
          .description(it.info.description)
          .type(it.codec.schema().toInputType())
          .build()
      )
    },
    ifPathCapture = {
      listOf(
        GraphQLArgument.Builder()
          .name(it.name)
          .description(it.info.description)
          .type(it.codec.schema().toInputType())
          .build()
      )
    },
    ifEmpty = { emptyList() },
    ifFixedMethod = { emptyList() },
    ifFixedPath = { emptyList() }
  )

fun <A> Schema<A>.toInputType(): GraphQLInputType =
  when (val type = this.schemaType) {
    SchemaType.SBoolean -> Scalars.GraphQLBoolean
    SchemaType.SInteger -> Scalars.GraphQLInt
    SchemaType.SNumber -> Scalars.GraphQLFloat
    SchemaType.SString ->
      Scalars.GraphQLString

    SchemaType.SDate -> Scalars.GraphQLString
    SchemaType.SDateTime -> Scalars.GraphQLString
    SchemaType.SBinary -> Scalars.GraphQLString
    is SchemaType.SArray -> GraphQLList.list(type.element.toInputType())
    is SchemaType.SObject.SProduct ->
      GraphQLInputObjectType.newInputObject()
        .name(type.info.fullName)
        .fields(
          type.fields.map { (name, schema) ->
            GraphQLInputObjectField.newInputObjectField()
              .name(name.name)
              .type(schema.toInputType())
//            .description()
              .build()
          }
        )
//    .description()
        .build()

    is SchemaType.SObject.SOpenProduct -> TODO()
    is SchemaType.SObject.SCoproduct -> TODO("???")
    is SchemaType.SRef -> TODO()
  }

fun <A> Schema<A>.toOutputType(): GraphQLOutputType =
  when (val type = this.schemaType) {
    SchemaType.SBoolean -> Scalars.GraphQLBoolean
    SchemaType.SInteger -> Scalars.GraphQLInt
    SchemaType.SNumber -> Scalars.GraphQLFloat
    SchemaType.SString -> Scalars.GraphQLString
    SchemaType.SDate -> Scalars.GraphQLString
    SchemaType.SDateTime -> Scalars.GraphQLString
    SchemaType.SBinary -> Scalars.GraphQLString
    is SchemaType.SArray -> GraphQLList.list(type.element.toOutputType())
    is SchemaType.SObject.SProduct ->
      GraphQLObjectType.newObject()
        .name(type.info.fullName)
        .fields(
          type.fields.map { (name, schema) ->
            GraphQLFieldDefinition.newFieldDefinition()
              .name(name.name)
              .type(schema.toOutputType())
//            .description()
              .build()
          }
        )
//    .description()
        .build()

    is SchemaType.SObject.SOpenProduct -> TODO("Map<String, schemaValue>")
    is SchemaType.SObject.SCoproduct -> TODO("Union")
    is SchemaType.SRef -> TODO("Ref???????")
  }

fun <A> Schema<A>.toFields(): List<GraphQLFieldDefinition> =
  when (val type = this.schemaType) {
    SchemaType.SBoolean -> listOf(
      GraphQLFieldDefinition.newFieldDefinition()
        .type(Scalars.GraphQLBoolean)
        .build()
    )
    SchemaType.SInteger -> listOf(
      GraphQLFieldDefinition.newFieldDefinition()

        .name("IntOutput")
        .type(Scalars.GraphQLInt)
        .build()
    )
    SchemaType.SNumber -> listOf(
      GraphQLFieldDefinition.newFieldDefinition()
        .type(Scalars.GraphQLFloat)
        .build()
    )
    SchemaType.SString -> listOf(
      GraphQLFieldDefinition.newFieldDefinition()

        .name("StringOutput")
        .type(Scalars.GraphQLString)
        .build()
    )
    SchemaType.SDate -> listOf(
      GraphQLFieldDefinition.newFieldDefinition()
        .type(Scalars.GraphQLString)
        .build()
    )
    SchemaType.SDateTime -> listOf(
      GraphQLFieldDefinition.newFieldDefinition()
        .type(Scalars.GraphQLString)
        .build()
    )
    SchemaType.SBinary -> listOf(
      GraphQLFieldDefinition.newFieldDefinition()
        .type(Scalars.GraphQLString)
        .build()
    )
    is SchemaType.SArray -> listOf(
      GraphQLFieldDefinition.newFieldDefinition()
        .type(GraphQLList.list(type.element.toInputType()))
        .build()
    )
    is SchemaType.SObject.SProduct ->
      type.fields.map { (name, schema) ->
        GraphQLFieldDefinition.newFieldDefinition()
          .name(name.name)
          .type(schema.toOutputType())
          .build()
      }

    is SchemaType.SObject.SOpenProduct -> TODO("Map<String, valueSchema>")
    is SchemaType.SObject.SCoproduct -> TODO("Union")
    is SchemaType.SRef -> TODO("Ref????")
  }

fun Schema<*>.name(): String? =
  when (val type = this.schemaType) {
    is SchemaType.SArray -> "Array of ${type.element.name()}"
    SchemaType.SBinary -> "Binary"
    SchemaType.SBoolean -> "Boolean"
    SchemaType.SDate -> "Date"
    SchemaType.SDateTime -> "DateTime"
    SchemaType.SInteger -> "Integer"
    SchemaType.SNumber -> "Float"
    is SchemaType.SObject.SCoproduct -> type.info.fullName
    is SchemaType.SObject.SOpenProduct -> type.info.fullName
    is SchemaType.SObject.SProduct -> type.info.fullName
    is SchemaType.SRef -> type.info.fullName
    SchemaType.SString -> "String"
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
