package com.fortysevendegrees.thool

import arrow.core.Tuple4
import arrow.core.Tuple5
import com.fortysevendegrees.thool.dsl.MethodSyntax

/**
 * @param I Input parameter types.
 * @param E Error output parameter types.
 * @param O Output parameter types.
 */
data class Endpoint<I, E, O>(
  val input: EndpointInput<I>,
  val errorOutput: EndpointOutput<E>,
  val output: EndpointOutput<O>,
  val info: EndpointInfo
) {
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
  fun renderPath(
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

  companion object : MethodSyntax {

    fun <I> input(input: EndpointInput<I>): Endpoint<I, Nothing, Unit> =
      Endpoint(input, EndpointOutput.Void(), EndpointOutput.empty(), EndpointInfo.empty())

    fun <E> error(output: EndpointOutput<E>): Endpoint<Unit, E, Unit> =
      Endpoint(EndpointInput.empty(), output, EndpointOutput.empty(), EndpointInfo.empty())

    fun <O> output(output: EndpointOutput<O>): Endpoint<Unit, Nothing, O> =
      Endpoint(EndpointInput.empty(), EndpointOutput.Void(), output, EndpointInfo.empty())
  }
}

fun <I, I2, E, O> Endpoint<I, E, O>.input(input: EndpointInput<I2>): Endpoint<Pair<I, I2>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInputLeftUnit")
fun <I, E, O> Endpoint<Unit, E, O>.input(input: EndpointInput<I>, dummmy: Unit = Unit): Endpoint<I, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInputRightUnit")
fun <I, E, O> Endpoint<I, E, O>.input(input: EndpointInput<Unit>, dummmy: Unit = Unit): Endpoint<I, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInputLeftRightUnit")
fun <E, O> Endpoint<Unit, E, O>.input(input: EndpointInput<Unit>, dummmy: Unit = Unit): Endpoint<Unit, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput2")
fun <I, I2, I3, E, O> Endpoint<Pair<I, I2>, E, O>.input(input: EndpointInput<I3>): Endpoint<Triple<I, I2, I3>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput2Pair")
fun <I, I2, I3, I4, E, O> Endpoint<Pair<I, I2>, E, O>.input(input: EndpointInput<Pair<I3, I4>>): Endpoint<Tuple4<I, I2, I3, I4>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput3")
fun <I, I2, I3, I4, E, O> Endpoint<Triple<I, I2, I3>, E, O>.input(input: EndpointInput<I4>): Endpoint<Tuple4<I, I2, I3, I4>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput4")
fun <I, I2, I3, I4, I5, E, O> Endpoint<Tuple4<I, I2, I3, I4>, E, O>.input(input: EndpointInput<I5>): Endpoint<Tuple5<I, I2, I3, I4, I5>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

@JvmName("withInput2RightUnit")
fun <I, I2, E, O> Endpoint<Pair<I, I2>, E, O>.input(input: EndpointInput<Unit>): Endpoint<Pair<I, I2>, E, O> =
  Endpoint(this@input.input.and(input), errorOutput, output, info)

fun <I, E, O, O2> Endpoint<I, E, O>.output(i: EndpointOutput<O2>): Endpoint<I, E, Pair<O, O2>> =
  Endpoint(input, errorOutput, output.and(i), info)

@JvmName("withOutputLeftUnit")
fun <I, E, O> Endpoint<I, E, Unit>.output(i: EndpointOutput<O>, dummy: Unit = Unit): Endpoint<I, E, O> =
  Endpoint(input, errorOutput, output.and(i), info)

@JvmName("withOutput3")
fun <I, E, O, O2, O3> Endpoint<I, E, Pair<O, O2>>.output(i: EndpointOutput<O3>): Endpoint<I, E, Triple<O, O2, O3>> =
  Endpoint(input, errorOutput, output.and(i), info)

data class EndpointInfo(
  val name: String?,
  val summary: String?,
  val description: String?,
  val tags: List<String>,
  val deprecated: Boolean
) {
  fun name(n: String): EndpointInfo = this.copy(name = n)
  fun summary(s: String): EndpointInfo = copy(summary = s)
  fun description(d: String): EndpointInfo = copy(description = d)
  fun tags(ts: List<String>): EndpointInfo = copy(tags = tags + ts)
  fun tag(t: String): EndpointInfo = copy(tags = tags + t)
  fun deprecated(d: Boolean): EndpointInfo = copy(deprecated = d)

  companion object {
    fun empty(): EndpointInfo =
      EndpointInfo(null, null, null, emptyList(), deprecated = false)
  }
}
