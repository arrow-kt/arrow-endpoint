package com.fortysevendeg.thool

import arrow.core.Either
import arrow.core.Tuple4
import arrow.core.Tuple5
import com.fortysevendeg.thool.dsl.MethodSyntax
import com.fortysevendeg.thool.server.ServerEndpoint

/**
 * An `Endpoint<Input, Error, Output>` for shape `suspend (Input) -> Either<Error, Output>` defines
 * how an endpoint receives [Input], and returns its [output] als in the case of an [errorOutput].
 * An [Endpoint] is considered to return an error when the [StatusCode] is not in the `2xx` range.
 *
 * As an example lets define a very simply endpoint: `GET /hello/world/{name}`
 *
 * ```kotlin
 * import com.fortysevendeg.thool.*
 *
 * val helloWorld: Endpoint<String, Unit, String> =
 *   Endpoint
 *     .get { "hello" / "world" / path("name", Codec.string) }
 *     .output(stringBody())
 * ```
 *
 * Here the path variable "name" is received as a simple `String`, and we the returned result is also `String`.
 * We can turn this into a [ServerEndpoint] by wiring it with `suspend (String) -> Either<Unit, String>`,
 * and we can immediately derive a client or docs from it without having to define ``suspend (String) -> Either<Unit, String>`.
 *
 * Let's see a slightly more advanced example where we want to receive `Person` and return a `User`.
 *   POST /register/person/{name}/{age}`
 *
 * ```kotlin
 * import com.fortysevendeg.thool.*
 * import kotlinx.serialization.Serializable
 * import kotlinx.serialization.decodeFromString
 * import kotlinx.serialization.encodeToString
 * import kotlinx.serialization.json.Json
 *
 * data class Person(val name: String, val age: Int)
 *
 * @Serializable data class User(val name: String, val age: Int) {
 *   companion object {
 *     val schema: Schema<User> = Schema.product(User::name to Schema.string, User::age to Schema.int)
 *     val jsonCodec = Codec.json(schema, { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }
 *   }
 * }
 *
 * val register: Endpoint<Person, Unit, User> =
 *   Endpoint
 *     .post { "register" / "person" }
 *     .input(
 *       path { path("name", Codec.string) / path("age", Codec.int) }
 *         .map({ (name, age) -> Person(name, age) }, { (name, age) -> Pair(name, age) })
 *     ).output(anyJsonBody(User.jsonCodec))
 * ```
 *
 * Here the inputs `name` and `age` are also path variables, but instead of working with `Pair<String, Int>` we map it into `Person`.
 * As output we've now defined a Json body instead of a simpler `String` body,
 * which requires us to pass `Codec` which can transform `User` into Json and back.
 * We leverage KotlinX Serialization in the example to do the Json parsing for us.
 *
 * We can now turn this into a [ServerEndpoint] by wiring it to `suspend (Person) -> Either<Unit, User>`,
 * and we can immediately derive a client or docs from it without having to define `suspend (Person) -> Either<Unit, User>`.
 *
 * Defining errors is as simple as defining an `output`, we can compose with our above defined endpoint:
 *
 * ```kotlin
 * @Serializable data class UserRegistrationFailed(val message: String) {
 *   companion object {
 *     val schema: Schema<UserRegistrationFailed> = Schema.product(UserRegistrationFailed::message to Schema.string)
 *     val jsonCodec = Codec.json(schema, { DecodeResult.Value(Json.decodeFromString(it)) }) { Json.encodeToString(it) }
 *   }
 * }
 *
 * val registerWithError: Endpoint<Person, UserRegistrationFailed, User> =
 *   registerPerson.errorOutput(anyJsonBody(UserRegistrationFailed.jsonCodec))
 * ```
 *
 * @param [input] defines how [Input] is defined in the http `Request` entity
 * @param [errorOutput] defines how [Error] will be defined in the http `Response` if the status code outside of the `2xx` range.
 * @param [output] defines how [Output] will be defined in http `Response` if the status code is within the `2xx` range.
 */
public data class Endpoint<Input, Error, Output>(
  val input: EndpointInput<Input>,
  val errorOutput: EndpointOutput<Error>,
  val output: EndpointOutput<Output>,
  val info: EndpointInfo
) {

  public fun name(n: String): Endpoint<Input, Error, Output> =
    this.copy(info = info.copy(name = n))

  public fun logic(f: suspend (Input) -> Either<Error, Output>): ServerEndpoint<Input, Error, Output> =
    ServerEndpoint(this, f)

  /**
   * Renders endpoint path, by default all parametrised path and query components are replaced by {param_name} or {paramN}.
   *
   * {{{
   * endpoint.in("p1" / path[String] / query[String]("par2"))
   * }}}
   * returns `/p1/{param1}?par2={par2}`
   *
   * @param includeAuth Should authentication inputs be included in the result.
   */
  public fun renderPath(
    renderPathParam: (Int, EndpointInput.PathCapture<*>) -> String =
      { index, pc -> pc.name?.let { name -> "{$name}" } ?: "{param$index}" },
    renderQueryParam: ((Int, EndpointInput.Query<*>) -> String)? = { _, q -> "${q.name}={${q.name}}" },
    includeAuth: Boolean = true
  ): String {
    val inputs = input.asListOfBasicInputs(includeAuth)
    val (pathComponents, pathParamCount) = renderedPathComponents(inputs, renderPathParam)
    val queryComponents = renderQueryParam
      ?.let { renderedQueryComponents(inputs, it, pathParamCount) }
      ?.joinToString("&") ?: ""

    return "/" + pathComponents.joinToString("/") + (if (queryComponents.isEmpty()) "" else "?$queryComponents")
  }

  private fun renderedPathComponents(
    inputs: List<EndpointInput.Basic<*, *, *>>,
    pathParamRendering: (Int, EndpointInput.PathCapture<*>) -> String
  ): Pair<List<String>, Int> =
    inputs.fold(Pair(emptyList(), 1)) { (acc, index), component ->
      when (component) {
        is EndpointInput.PathCapture<*> -> Pair(acc + pathParamRendering(index, component), index + 1)
        is EndpointInput.FixedPath -> Pair(acc + component.s, index)
        else -> Pair(acc, index)
      }
    }

  private fun renderedQueryComponents(
    inputs: List<EndpointInput.Basic<*, *, *>>,
    queryParamRendering: (Int, EndpointInput.Query<*>) -> String,
    pathParamCount: Int
  ): List<String> = inputs.fold(Pair(emptyList<String>(), pathParamCount)) { (acc, index), component ->
    when (component) {
      is EndpointInput.Query<*> -> Pair(acc + queryParamRendering(index, component), index + 1)
      else -> Pair(acc, index)
    }
  }.first

  /**
   * Detailed description of the endpoint, with inputs/outputs represented in the same order as originally defined,
   * including mapping information.
   */
  public fun details(): String =
    "Endpoint${info.name?.let { "[$it]" } ?: ""}(input: $input, errorOutput: $errorOutput, output: $output)"

  public companion object : MethodSyntax {

    public fun <Input> input(input: EndpointInput<Input>): Endpoint<Input, Unit, Unit> =
      Endpoint(input, EndpointOutput.empty(), EndpointOutput.empty(), EndpointInfo.empty())

    public fun <Error> error(output: EndpointOutput<Error>): Endpoint<Unit, Error, Unit> =
      Endpoint(EndpointInput.empty(), output, EndpointOutput.empty(), EndpointInfo.empty())

    public fun <Output> output(output: EndpointOutput<Output>): Endpoint<Unit, Unit, Output> =
      Endpoint(EndpointInput.empty(), EndpointOutput.empty(), output, EndpointInfo.empty())
  }
}

public fun <I, I2, E, O> Endpoint<I, E, O>.input(input: EndpointInput<I2>): Endpoint<Pair<I, I2>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInputLeftUnit")
public fun <I, E, O> Endpoint<Unit, E, O>.input(input: EndpointInput<I>, dummmy: Unit = Unit): Endpoint<I, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInputRightUnit")
public fun <I, E, O> Endpoint<I, E, O>.input(input: EndpointInput<Unit>, dummmy: Unit = Unit): Endpoint<I, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInputLeftRightUnit")
public fun <E, O> Endpoint<Unit, E, O>.input(input: EndpointInput<Unit>, dummmy: Unit = Unit): Endpoint<Unit, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput2")
public fun <I, I2, I3, E, O> Endpoint<Pair<I, I2>, E, O>.input(input: EndpointInput<I3>): Endpoint<Triple<I, I2, I3>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput2Pair")
public fun <I, I2, I3, I4, E, O> Endpoint<Pair<I, I2>, E, O>.input(input: EndpointInput<Pair<I3, I4>>): Endpoint<Tuple4<I, I2, I3, I4>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput3")
public fun <I, I2, I3, I4, E, O> Endpoint<Triple<I, I2, I3>, E, O>.input(input: EndpointInput<I4>): Endpoint<Tuple4<I, I2, I3, I4>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput4")
public fun <I, I2, I3, I4, I5, E, O> Endpoint<Tuple4<I, I2, I3, I4>, E, O>.input(input: EndpointInput<I5>): Endpoint<Tuple5<I, I2, I3, I4, I5>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput2RightUnit")
public fun <I, I2, E, O> Endpoint<Pair<I, I2>, E, O>.input(input: EndpointInput<Unit>): Endpoint<Pair<I, I2>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

public fun <I, E, O, O2> Endpoint<I, E, O>.output(i: EndpointOutput<O2>): Endpoint<I, E, Pair<O, O2>> =
  Endpoint(input, errorOutput, output.and(i), info)

@JvmName("withOutputLeftUnit")
public fun <I, E, O> Endpoint<I, E, Unit>.output(i: EndpointOutput<O>, dummy: Unit = Unit): Endpoint<I, E, O> =
  Endpoint(input, errorOutput, output.and(i), info)

@JvmName("withOutput3")
public fun <I, E, O, O2, O3> Endpoint<I, E, Pair<O, O2>>.output(i: EndpointOutput<O3>): Endpoint<I, E, Triple<O, O2, O3>> =
  Endpoint(input, errorOutput, output.and(i), info)

public fun <I, E, E2, O> Endpoint<I, E, O>.errorOutput(i: EndpointOutput<E2>): Endpoint<I, Pair<E, E2>, O> =
  Endpoint(input, errorOutput.and(i), output, info)

@JvmName("withErrorOutputLeftUnit")
public fun <I, E, O> Endpoint<I, Unit, O>.errorOutput(i: EndpointOutput<E>, dummy: Unit = Unit): Endpoint<I, E, O> =
  Endpoint(input, errorOutput.and(i), output, info)

@JvmName("withErrorOutput3")
public fun <I, E, E2, E3, O> Endpoint<I, Pair<E, E2>, O>.output(i: EndpointOutput<E3>): Endpoint<I, Triple<E, E2, E3>, O> =
  Endpoint(input, errorOutput.and(i), output, info)

public data class EndpointInfo(
  val name: String?,
  val summary: String?,
  val description: String?,
  val tags: List<String>,
  val deprecated: Boolean
) {
  public fun name(n: String): EndpointInfo = this.copy(name = n)
  public fun summary(s: String): EndpointInfo = copy(summary = s)
  public fun description(d: String): EndpointInfo = copy(description = d)
  public fun tags(ts: List<String>): EndpointInfo = copy(tags = tags + ts)
  public fun tag(t: String): EndpointInfo = copy(tags = tags + t)
  public fun deprecated(d: Boolean): EndpointInfo = copy(deprecated = d)

  public companion object {
    public fun empty(): EndpointInfo =
      EndpointInfo(null, null, null, emptyList(), deprecated = false)
  }
}
