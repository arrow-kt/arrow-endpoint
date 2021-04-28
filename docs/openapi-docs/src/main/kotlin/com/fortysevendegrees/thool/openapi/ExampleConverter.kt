package com.fortysevendegrees.thool.openapi

import arrow.core.Either
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.EndpointIO


internal object ExampleConverter {
  data class Examples(val singleExample: ExampleValue?, val multipleExamples: Map<String, ReferenceOr<Example>>)

  fun <A> convertExamples(codec: Codec<*, A, *>, examples: List<EndpointIO.Info.Example<A>>): Examples =
    convertExamples(examples) { ExampleValue(codec, it) }

  private fun <A> convertExamples(
    examples: List<EndpointIO.Info.Example<A>>,
    exampleValue: (value: A) -> ExampleValue?
  ): Examples =
    when (examples.size) {
      1 -> Examples(exampleValue(examples.first().value), emptyMap())
      else -> {
        val exampleValues = examples.mapIndexed { index, example ->
          val name = example.name ?: "Example$index"
          val example: ReferenceOr<Example> = Either.Right(
            Example(
              summary = example.summary,
              description = null,
              value = exampleValue(example.value),
              externalValue = null
            )
          )
          name to example
        }.toMap()
        Examples(null, exampleValues)
      }
    }
}
