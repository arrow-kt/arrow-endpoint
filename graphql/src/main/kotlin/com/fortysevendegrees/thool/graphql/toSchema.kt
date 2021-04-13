import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.EndpointOutput
import com.fortysevendegrees.thool.Schema
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

/**
 * The conversion rules are the following:
 *   - GET endpoints are turned into Queries
 *   - PUT, POST and DELETE endpoints are turned into Mutations
 *   - fixed query paths are used to name GraphQL fields (e.g. an endpoint /book/add will give a GraphQL field named bookAdd)
 *   - query parameters, headers, cookies and request body are used as GraphQL arguments
 */
fun List<Endpoint<*, Nothing, *>>.toSchema(): GraphQLSchema {
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

fun <I, E, O> Endpoint<I, E, O>.generateFunction(

): GraphQLFieldDefinition =
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
          .name(it.codec.schema().name())
          .fields(it.codec.schema().toFields())
          .build()
      )
    },
    ifStatusCode = {
      it.codec.schema().toScalarOrNull()?.let(::listOf) ?: listOf(
        GraphQLObjectType.Builder()
          .description(it.info.description)
          .name(it.codec.schema().name())
          .fields(it.codec.schema().toFields())
          .build()
      )
    },
    ifFixedStatuscode = {
      it.codec.schema().toScalarOrNull()?.let(::listOf) ?: listOf(
        GraphQLObjectType.Builder()
          .description(it.info.description)
          .name(it.codec.schema().name())
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
          .name(it.codec.schema().name())
          .fields(it.codec.schema().toFields())
          .build()
      )
    },
    ifStreamBody = { TODO("Subscription???") }
  )

fun Schema<*>.toScalarOrNull(): GraphQLScalarType? =
  when (this) {
    is Schema.Boolean -> Scalars.GraphQLBoolean
    is Schema.String -> Scalars.GraphQLString

    is Schema.Number.Byte -> Scalars.GraphQLInt
    is Schema.Number.Int -> Scalars.GraphQLInt
    is Schema.Number.Short -> Scalars.GraphQLInt
    is Schema.Number.UByte -> Scalars.GraphQLInt
    is Schema.Number.UShort -> Scalars.GraphQLInt
    is Schema.Number.Float -> Scalars.GraphQLFloat
    is Schema.Number.Double -> Scalars.GraphQLFloat

    is Schema.Binary -> Scalars.GraphQLString
    is Schema.Date -> Scalars.GraphQLString
    is Schema.DateTime -> Scalars.GraphQLString

    is Schema.Nullable -> this.element.toScalarOrNull()

    // TODO Check unsigned numbers
    is Schema.Number.Long -> Scalars.GraphQLInt
    is Schema.Number.ULong -> Scalars.GraphQLInt
    is Schema.Number.UInt -> Scalars.GraphQLInt

    is Schema.Coproduct -> null
    is Schema.Either -> null
    is Schema.Enum -> null
    is Schema.List -> null
    is Schema.Map -> null
    is Schema.OpenProduct -> null
    is Schema.Product -> null
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
  when (this) {
    is Schema.Boolean -> Scalars.GraphQLBoolean
    is Schema.String -> Scalars.GraphQLString
    is Schema.Number.Byte -> Scalars.GraphQLInt
    is Schema.Number.Int -> Scalars.GraphQLInt
    is Schema.Number.Short -> Scalars.GraphQLInt
    is Schema.Number.UByte -> Scalars.GraphQLInt
    is Schema.Number.UShort -> Scalars.GraphQLInt
    is Schema.Number.Float -> Scalars.GraphQLFloat
    is Schema.Number.Double -> Scalars.GraphQLFloat
    is Schema.Binary -> Scalars.GraphQLString
    is Schema.Date -> Scalars.GraphQLString
    is Schema.DateTime -> Scalars.GraphQLString
    is Schema.Nullable -> this.element.toInputType()

    // TODO Check unsigned numbers
    is Schema.Number.Long -> Scalars.GraphQLInt
    is Schema.Number.ULong -> Scalars.GraphQLInt
    is Schema.Number.UInt -> Scalars.GraphQLInt

    is Schema.List -> GraphQLList.list(element.toInputType())
    is Schema.Product ->
      GraphQLInputObjectType.newInputObject()
        .name(objectInfo.fullName)
        .fields(
          fields.map { (name, schema) ->
            GraphQLInputObjectField.newInputObjectField()
              .name(name.name)
              .type(schema.toInputType())
              .build()
          }
        )
        .build()

    is Schema.Map -> TODO("Map<keySchema, schemaValue>")
    is Schema.OpenProduct -> TODO("Map<String, schemaValue>")
    is Schema.Enum -> TODO("Union")
    is Schema.Either -> TODO("Union")
    is Schema.Coproduct -> TODO("Union")
  }

fun <A> Schema<A>.toOutputType(): GraphQLOutputType =
  when (this) {
    is Schema.Boolean -> Scalars.GraphQLBoolean
    is Schema.String -> Scalars.GraphQLString
    is Schema.Number.Byte -> Scalars.GraphQLInt
    is Schema.Number.Int -> Scalars.GraphQLInt
    is Schema.Number.Short -> Scalars.GraphQLInt
    is Schema.Number.UByte -> Scalars.GraphQLInt
    is Schema.Number.UShort -> Scalars.GraphQLInt
    is Schema.Number.Float -> Scalars.GraphQLFloat
    is Schema.Number.Double -> Scalars.GraphQLFloat
    is Schema.Binary -> Scalars.GraphQLString
    is Schema.Date -> Scalars.GraphQLString
    is Schema.DateTime -> Scalars.GraphQLString
    is Schema.Nullable -> this.element.toOutputType()

    // TODO Check unsigned numbers
    is Schema.Number.Long -> Scalars.GraphQLInt
    is Schema.Number.ULong -> Scalars.GraphQLInt
    is Schema.Number.UInt -> Scalars.GraphQLInt

    is Schema.List -> GraphQLList.list(element.toInputType())
    is Schema.Product ->
      GraphQLObjectType.newObject()
        .name(objectInfo.fullName)
        .fields(
          fields.map { (name, schema) ->
            GraphQLFieldDefinition.newFieldDefinition()
              .name(name.name)
              .type(schema.toOutputType())
//            .description()
              .build()
          }
        )
//    .description()
        .build()

    is Schema.Coproduct -> TODO("Union")
    is Schema.Either -> TODO("Union")
    is Schema.Enum -> TODO("Union")
    is Schema.Map -> TODO("Map<String, schemaValue>")
    is Schema.OpenProduct -> TODO("Map<A, schemaValue>")
  }

val booleanOut = listOf(
  GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLBoolean)
    .name("BooleanOut")
    .build()
)
val intOut = listOf(
  GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLInt)
    .name("IntOut")
    .build()
)
val floatOut = listOf(
  GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLFloat)
    .name("FloatOut")
    .build()
)
val stringOut = listOf(
  GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLString)
    .name("StringOut")
    .build()
)

fun <A> Schema<A>.toFields(): List<GraphQLFieldDefinition> =
  when (this) {
    is Schema.Binary -> stringOut
    is Schema.String -> stringOut
    is Schema.Date -> stringOut
    is Schema.DateTime -> stringOut
    is Schema.Boolean -> booleanOut
    is Schema.Number.Byte -> intOut
    is Schema.Number.Int -> intOut
    is Schema.Number.Short -> intOut
    is Schema.Number.UByte -> intOut
    is Schema.Number.UShort -> intOut
    is Schema.Number.Float -> floatOut
    is Schema.Number.Double -> floatOut

    // TODO Check unsigned numbers
    is Schema.Number.Long -> intOut
    is Schema.Number.UInt -> intOut
    is Schema.Number.ULong -> intOut

    is Schema.Nullable -> element.toFields()
    is Schema.List -> listOf(
      GraphQLFieldDefinition.newFieldDefinition()
        .type(GraphQLList.list(element.toInputType()))
        .build()
    )

    is Schema.Product ->
      fields.map { (name, schema) ->
        GraphQLFieldDefinition.newFieldDefinition()
          .name(name.name)
          .type(schema.toOutputType())
          .build()
      }
    is Schema.OpenProduct -> TODO("Map<String, valuSchema>")
    is Schema.Map -> TODO("Map<keySchema, valueSchema>")
    is Schema.Either -> TODO("Union")
    is Schema.Enum -> TODO("Union")
    is Schema.Coproduct -> TODO("Union")
  }

fun Schema<*>.name(): String =
  when (this) {
    is Schema.Binary -> "Binary"
    is Schema.Boolean -> "Boolean"
    is Schema.Coproduct -> objectInfo.fullName
    is Schema.Date -> "Date"
    is Schema.DateTime -> "DateTime"
    is Schema.Either -> "Either<${left.name()}, ${right.name()}>"
    is Schema.Enum -> objectInfo.fullName
    is Schema.List -> "Array of ${element.name()}"
    is Schema.Map -> this.toString()
    is Schema.Nullable -> element.name()
    is Schema.Number.Byte -> "Byte"
    is Schema.Number.Double -> "Double"
    is Schema.Number.Float -> "Float"
    is Schema.Number.Int -> "Int"
    is Schema.Number.Long -> "Long"
    is Schema.Number.Short -> "Short"
    is Schema.Number.UByte -> "UByte"
    is Schema.Number.UInt -> "UInt"
    is Schema.Number.ULong -> "ULong"
    is Schema.Number.UShort -> "UShort"
    is Schema.OpenProduct -> objectInfo.fullName
    is Schema.Product -> objectInfo.fullName
    is Schema.String -> "String"
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
