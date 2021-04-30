package com.fortysevendegrees.thool.server.interpreter

import com.fortysevendegrees.thool.server.interpreter.DecodeBasicInputsResult.Failure
import arrow.core.tail
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.DecodeResult.Failure.Multiple
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.basicInputSortIndex
import com.fortysevendegrees.thool.headAndTailOrNull
import com.fortysevendegrees.thool.initAndLastOrNull
import com.fortysevendegrees.thool.updated
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.QueryParams
import com.fortysevendegrees.thool.model.ServerRequest
import com.fortysevendegrees.thool.model.headers

public sealed interface DecodeBasicInputsResult {

  /** @param basicInputsValues Values of basic inputs, in order as they are defined in the endpoint. */
  public data class Values(
    val basicInputsValues: List<Any?>,
    val bodyInputWithIndex: Pair<EndpointIO.Body<*, *>, Int>?
  ) : DecodeBasicInputsResult {

    private fun verifyNoBody(input: EndpointInput<*>): Unit =
      check(bodyInputWithIndex == null) { "Double body definition: $input" }

    fun addBodyInput(input: EndpointIO.Body<*, *>, bodyIndex: Int): Values {
      verifyNoBody(input)
      return copy(bodyInputWithIndex = Pair(input, bodyIndex))
    }

    /** Sets the value of the body input, once it is known, if a body input is defined. */
    fun setBodyInputValue(v: Any?): Values =
      when (bodyInputWithIndex) {
        null -> this
        else -> copy(basicInputsValues = basicInputsValues.updated(bodyInputWithIndex.second, v))
      }

    fun setBasicInputValue(v: Any?, i: Int): Values =
      copy(basicInputsValues = basicInputsValues.updated(i, v))
  }

  public data class Failure(val input: EndpointInput.Basic<*, *, *>, val failure: DecodeResult.Failure) :
    DecodeBasicInputsResult
}

internal data class DecodeInputsContext(val request: ServerRequest, val pathSegments: List<String>) {
  fun method(): Method = request.method

  fun nextPathSegment(): Pair<String?, DecodeInputsContext> =
    when {
      pathSegments.isEmpty() -> Pair(null, this)
      else -> Pair(pathSegments.first(), DecodeInputsContext(request, pathSegments.tail()))
    }

  fun header(name: String): List<String> =
    request.headers.headers(name)

  fun headers(): List<Pair<String, String>> =
    request.headers.map { h -> Pair(h.name, h.value) }

  fun queryParameter(name: String): List<String> =
    queryParameters.getMulti(name) ?: emptyList()

  val queryParameters: QueryParams = request.queryParameters
}

object DecodeBasicInputs {
  private data class IndexedBasicInput(val input: EndpointInput.Basic<*, *, *>, val index: Int)

  /**
   * Decodes values of all basic inputs defined by the given `input`, and returns a map from the input to the input's value.
   *
   * An exception is the body input, which is not decoded. This is because typically bodies can be only read once.
   * That's why, all non-body inputs are used to decide if a request matches the endpoint, or not.
   * If a body input is present, it is also returned as part of the result.
   *
   * In any of the decoding fails, the failure is returned together , the failing input.
   */
  // TODO rename
  fun apply(input: EndpointInput<*>, request: ServerRequest): DecodeBasicInputsResult =
    apply(input, DecodeInputsContext(request, request.pathSegments))

  // TODO rename
  private fun apply(input: EndpointInput<*>, ctx: DecodeInputsContext): DecodeBasicInputsResult {
    // The first decoding failure is returned.
    // We decode in the following order: method, path, query, headers (incl. cookies), request, status, body
    // An exact-path check is done after thool.method & path matching

    val basicInputs = input.asListOfBasicInputs().mapIndexed { index, input -> IndexedBasicInput(input, index) }

    val methodInputs = basicInputs.filter { (input, _) -> isRequestMethod(input) }
    val pathInputs = basicInputs.filter { (input, _) -> isPath(input) }
    val otherInputs = basicInputs
      .filterNot { (input, _) -> isRequestMethod(input) || isPath(input) }
      .sortedBy { (input, _) -> basicInputSortIndex(input) }

    // we're using null as a placeholder for the future values. All except the body (which is determined by
    // interpreter-specific code), should be filled by the end of this method.
    // TODO rewrite to functions without currying
    return compose(
      whenOthers(methodInputs),
      whenPath(pathInputs),
      whenOthers(otherInputs)
    ).invoke(DecodeBasicInputsResult.Values(List(basicInputs.size) { null }, null), ctx).first
  }

  /**
   * We're decoding paths differently than other inputs.
   * We first map all path segments to their decoding results (not checking if this is a successful or failed decoding at this stage).
   * This is collected as the `decodedPathInputs` value.
   *
   * Once this is done, we check if there are remaining path segments. If yes - the decoding fails , a `Mismatch`.
   *
   * Hence, a failure due to a mismatch in the number of segments takes **priority** over any potential failures in decoding the segments.
   */
  private fun whenPath(pathInputs: List<IndexedBasicInput>): (DecodeBasicInputsResult.Values, DecodeInputsContext) -> Pair<DecodeBasicInputsResult, DecodeInputsContext> =
    { decodeValues, ctx ->
      when (val res = pathInputs.initAndLastOrNull()) {
        // Match everything if no path input is specified
        null -> Pair(decodeValues, ctx)
        else -> matchPathInner(
          pathInputs = pathInputs,
          ctx = ctx,
          decodeValues = decodeValues,
          decodedPathInputs = emptyList(),
          lastPathInput = res.second
        )
      }
    }

  private tailrec fun matchPathInner(
    pathInputs: List<IndexedBasicInput>,
    ctx: DecodeInputsContext,
    decodeValues: DecodeBasicInputsResult.Values,
    decodedPathInputs: List<Pair<IndexedBasicInput, DecodeResult<*>>>,
    lastPathInput: IndexedBasicInput
  ): Pair<DecodeBasicInputsResult, DecodeInputsContext> =
    when (val res = pathInputs.headAndTailOrNull()) {
      null -> {
        val (extraSegmentOpt, newCtx) = ctx.nextPathSegment()
        when (extraSegmentOpt) {
          null -> Pair(foldDecodedPathInputs(decodedPathInputs, decodeValues), newCtx)
          else -> // shape path mismatch - input path too long; there are more segments in the request path than expected by that input. Reporting a failure on the last path input.
            Pair(Failure(lastPathInput.input, Multiple(collectRemainingPath(emptyList(), ctx).first)), newCtx)
        }
      }
      else -> {
        val (idxInput, restInputs) = res
        when (val input = idxInput.input) {
          is EndpointInput.FixedPath -> {
            val (nextSegment, newCtx) = ctx.nextPathSegment()
            when (nextSegment) {
              null -> if (input.s.isEmpty()) { // FixedPath("") matches an empty path
                val newDecodedPathInputs = decodedPathInputs + Pair(idxInput, input.codec.decode(Unit))
                matchPathInner(restInputs, newCtx, decodeValues, newDecodedPathInputs, idxInput)
              } else { // shape path mismatch - input path too short
                Pair(Failure(idxInput.input, DecodeResult.Failure.Missing), newCtx)
              }
              else -> {
                if (nextSegment == input.s) {
                  val newDecodedPathInputs = decodedPathInputs + Pair(idxInput, input.codec.decode(Unit))
                  matchPathInner(restInputs, newCtx, decodeValues, newDecodedPathInputs, idxInput)
                } else {
                  Pair(Failure(input, DecodeResult.Failure.Mismatch(input.s, nextSegment)), newCtx)
                }
              }
            }
          }
          is EndpointInput.PathCapture -> {
            val (nextSegment, newCtx) = ctx.nextPathSegment()
            when (nextSegment) {
              null -> Pair(Failure(input, DecodeResult.Failure.Missing), newCtx)
              else -> {
                val newDecodedPathInputs = decodedPathInputs + Pair(idxInput, input.codec.decode(nextSegment))
                matchPathInner(restInputs, newCtx, decodeValues, newDecodedPathInputs, idxInput)
              }
            }
          }
          is EndpointInput.PathsCapture -> {
            val (paths, newCtx) = collectRemainingPath(emptyList(), ctx)
            val newDecodedPathInputs = decodedPathInputs + Pair(idxInput, input.codec.decode(paths.toList()))
            matchPathInner(restInputs, newCtx, decodeValues, newDecodedPathInputs, idxInput)
          }
          else -> throw IllegalStateException("Unexpected EndpointInput $input encountered. This is most likely a bug in the library")
        }
      }
    }

  private tailrec fun foldDecodedPathInputs(
    decodedPathInputs: List<Pair<IndexedBasicInput, DecodeResult<*>>>,
    acc: DecodeBasicInputsResult.Values
  ): DecodeBasicInputsResult =
    when (val res = decodedPathInputs.headAndTailOrNull()) {
      null -> acc
      else -> {
        val (t, ts) = res
        val (input, result) = t
        when (result) {
          is DecodeResult.Failure -> Failure(input.input, result)
          is DecodeResult.Value -> foldDecodedPathInputs(ts, acc.setBasicInputValue(result.value, input.index))
        }
      }
    }

  private tailrec fun collectRemainingPath(
    acc: List<String>,
    c: DecodeInputsContext
  ): Pair<List<String>, DecodeInputsContext> {
    val (str, c2) = c.nextPathSegment()
    return when {
      str != null -> collectRemainingPath(acc + str, c2)
      else -> Pair(acc, c2)
    }
  }

  private fun whenOthers(inputs: List<IndexedBasicInput>): (DecodeBasicInputsResult.Values, DecodeInputsContext) -> Pair<DecodeBasicInputsResult, DecodeInputsContext> =
    { decodeValues, ctx -> _whenOthers(inputs, decodeValues, ctx) }

  private tailrec fun _whenOthers(
    inputs: List<IndexedBasicInput>,
    values: DecodeBasicInputsResult.Values,
    ctx: DecodeInputsContext
  ): Pair<DecodeBasicInputsResult, DecodeInputsContext> =
    when (val res = inputs.headAndTailOrNull()) {
      null -> Pair(values, ctx)
      else -> {
        val (input, index) = res.first
        val tail = res.second
        when (input) {
          is EndpointIO.Body<*, *> -> _whenOthers(res.second, values.addBodyInput(input, index), ctx)
          else -> {
            val (result, ctx2) = whenOther(input, ctx)
            when (result) {
              is DecodeResult.Value -> _whenOthers(
                tail,
                values.setBasicInputValue(result.value, index),
                ctx2
              )
              is DecodeResult.Failure -> Pair(Failure(input, result), ctx2)
            }
          }
        }
      }
    }

  private fun whenOther(
    input: EndpointInput.Basic<*, *, *>,
    ctx: DecodeInputsContext
  ): Pair<DecodeResult<*>, DecodeInputsContext> =
    when (input) {
      is EndpointInput.FixedMethod ->
        if (input.m == ctx.method()) Pair(input.codec.decode(Unit), ctx)
        else Pair(DecodeResult.Failure.Mismatch(input.m.value, ctx.method().value), ctx)

      is EndpointInput.Query -> Pair(input.codec.decode(ctx.queryParameter(input.name)), ctx)
      is EndpointInput.QueryParams -> Pair(input.codec.decode(ctx.queryParameters), ctx)
      is EndpointIO.Header -> Pair(input.codec.decode(ctx.header(input.name)), ctx)
      is EndpointIO.Empty -> Pair(input.codec.decode(Unit), ctx)

      is EndpointInput.FixedPath -> TODO()
      is EndpointIO.Body<*, *> -> TODO()
      is EndpointInput.Cookie -> TODO()
      is EndpointInput.PathCapture -> TODO()
      is EndpointInput.PathsCapture -> TODO()
    }

  private fun isRequestMethod(basic: EndpointInput.Basic<*, *, *>): Boolean =
    when (basic) {
      is EndpointInput.FixedMethod<*> -> true
      else -> false
    }

  private fun isPath(basic: EndpointInput.Basic<*, *, *>): Boolean =
    when (basic) {
      is EndpointInput.FixedPath<*> -> true
      is EndpointInput.PathCapture<*> -> true
      is EndpointInput.PathsCapture<*> -> true
      else -> false
    }

  private fun compose(vararg fs: DecodeInputResultTransform): DecodeInputResultTransform = { values, ctx ->
    when {
      fs.isNotEmpty() -> {
        val ff = fs.first()
        val res = ff(values, ctx)
        val (values2, ctx2) = res
        when (values2) {
          is DecodeBasicInputsResult.Values -> compose(*fs.drop(1).toTypedArray()).invoke(values2, ctx2)
          else -> res
        }
      }
      else -> Pair(values, ctx)
    }
  }
}

private typealias DecodeInputResultTransform = (DecodeBasicInputsResult.Values, DecodeInputsContext) -> Pair<DecodeBasicInputsResult, DecodeInputsContext>
