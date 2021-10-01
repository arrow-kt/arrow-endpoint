package arrow.endpoint.server.interpreter

import arrow.endpoint.CombineParams
import arrow.endpoint.DecodeResult
import arrow.endpoint.EndpointIO
import arrow.endpoint.EndpointInput
import arrow.endpoint.Mapping
import arrow.endpoint.Params
import arrow.endpoint.headAndTailOrNull

public sealed interface InputValueResult {
  public data class Value(val params: Params, val remainingBasicValues: List<Any?>) : InputValueResult
  public data class Failure(val input: EndpointInput<*>, val failure: DecodeResult.Failure) : InputValueResult

  public companion object {

    /**
     * Returns the value of the input, tupled and mapped as described by the data structure. Values of basic inputs
     * are taken as consecutive values from `values.basicInputsValues`. Hence, these should when (in order).
     */
    public fun from(input: EndpointInput<*>, values: DecodeBasicInputsResult.Values): InputValueResult =
      from(input, values.basicInputsValues)

    @Suppress("UNCHECKED_CAST")
    private fun from(input: EndpointInput<*>, remainingBasicValues: List<Any?>): InputValueResult =
      when (input) {
        is EndpointInput.Pair<*, *, *> -> handlePair(input.first, input.second, input.combine, remainingBasicValues)
        is EndpointIO.Pair<*, *, *> -> handlePair(input.first, input.second, input.combine, remainingBasicValues)
        is EndpointInput.MappedPair<*, *, *, *> ->
          handleMappedPair(
            input.input as EndpointInput<Any?>,
            input.mapping as Mapping<Any?, Any?>,
            remainingBasicValues
          )
        is EndpointIO.MappedPair<*, *, *, *> -> handleMappedPair(
          input.wrapped as EndpointInput<Any?>,
          input.mapping as Mapping<Any?, Any?>,
          remainingBasicValues
        )
//      is EndpointInput.Auth<*>  -> from(auth.input, remainingBasicValues)
        is EndpointInput.Basic<*, *, *> ->
          remainingBasicValues.headAndTailOrNull()?.let { (v, valuesTail) ->
            Value(Params.ParamsAsAny(v), valuesTail)
          }
            ?: throw IllegalStateException("Mismatch between basic input values: $remainingBasicValues, and basic inputs in: $input")
      }

    private fun handlePair(
      left: EndpointInput<*>,
      right: EndpointInput<*>,
      combine: CombineParams,
      remainingBasicValues: List<Any?>
    ): InputValueResult =
      when (val res = from(left, remainingBasicValues)) {
        is Value ->
          when (val res2 = from(right, res.remainingBasicValues)) {
            is Value ->
              Value(combine(res.params, res2.params), res2.remainingBasicValues)
            is Failure -> res2
          }
        is Failure -> res
      }

    @Suppress("UNCHECKED_CAST")
    private fun <II, T> handleMappedPair(
      wrapped: EndpointInput<II>,
      codec: Mapping<II, T>,
      remainingBasicValues: List<Any?>
    ): InputValueResult =
      when (val res = from(wrapped, remainingBasicValues)) {
        is Value -> when (val res2 = codec.decode(res.params.asAny as II)) {
          is DecodeResult.Value -> Value(
            Params.ParamsAsAny(res2.value),
            res.remainingBasicValues
          )
          is DecodeResult.Failure -> Failure(wrapped, res2)
        }
        is Failure -> res
      }
  }
}
