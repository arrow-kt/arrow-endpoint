import com.fortysevendegrees.tapir.CombineParams
import com.fortysevendegrees.tapir.DecodeResult
import com.fortysevendegrees.tapir.EndpointInput
import com.fortysevendegrees.tapir.Mapping
import com.fortysevendegrees.tapir.Params
import com.fortysevendegrees.tapir.headAndTailOrNull

sealed interface InputValueResult {
  data class Value(val params: Params, val remainingBasicValues: List<Any?>) : InputValueResult
  data class Failure(val input: EndpointInput<*>, val failure: DecodeResult.Failure) : InputValueResult

  companion object {

    /**
     * Returns the value of the input, tupled and mapped as described by the data structure. Values of basic inputs
     * are taken as consecutive values from `values.basicInputsValues`. Hence, these should when (in order).
     */
    fun from(input: EndpointInput<*>, values: DecodeBasicInputsResult.Values): InputValueResult =
      from(input, values.basicInputsValues)

    private fun from(input: EndpointInput<*>, remainingBasicValues: List<Any?>): InputValueResult =
      when (input) {
        is EndpointInput.Pair<*, *, *> -> println("EndpointInput.Pair(${input.first}, ${input.second}) - $remainingBasicValues").let {
          handlePair(
            input.first,
            input.second,
            input.combine,
            remainingBasicValues
          )
        }
//      is com.fortysevendegrees.tapir.EndpointIO.Pair<*, *>    -> handlePair(left, right, combine, remainingBasicValues)
        is EndpointInput.MappedPair<*, *, *, *> -> println("InputValueResult.from.EndpointInput.MappedPair<*, *, *>").let {
          handleMappedPair(
            input.input as EndpointInput<Any?>,
            input.mapping as Mapping<Any?, Any?>,
            remainingBasicValues
          )
        }
//      is com.fortysevendegrees.tapir.EndpointIO.MappedPair<*, *, *>       -> handleMappedPair(wrapped, codec, remainingBasicValues)
//      auth: com.fortysevendegrees.tapir.EndpointInput.Auth<_>                 -> apply(auth.input, remainingBasicValues)
        is EndpointInput.Basic<*, *, *> ->
          remainingBasicValues.headAndTailOrNull()?.let { (v, valuesTail) ->
            println("InputValueResult.from.EndpointInput.Basic<*>: v: $v, valuesTail: $valuesTail")
            Value(Params.ParamsAsAny(v), valuesTail)
          }
            ?: throw IllegalStateException("Mismatch between basic input values: $remainingBasicValues, and basic inputs in: $input")
//      is com.fortysevendegrees.tapir.EndpointInput.MappedTriple -> TODO()
//      is com.fortysevendegrees.tapir.EndpointInput.MappedTuple4 -> TODO()
//      is com.fortysevendegrees.tapir.EndpointInput.Triple -> TODO()
//      is com.fortysevendegrees.tapir.EndpointInput.Tuple4 -> TODO()
        else -> TODO()
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
