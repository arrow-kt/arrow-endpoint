package arrow.endpoint.docs.openapi

import arrow.endpoint.Codec
import arrow.endpoint.EndpointIO

internal data class Examples(val singleExample: ExampleValue?, val multipleExamples: Map<String, Referenced<Example>>)

internal fun <A> List<EndpointIO.Info.Example<A>>.toExamples(codec: Codec<*, A, *>): Examples =
  convertExamples { ExampleValue(codec, it) }

private fun <A> List<EndpointIO.Info.Example<A>>.convertExamples(exampleValue: (value: A) -> ExampleValue?): Examples =
  when (size) {
    1 -> Examples(exampleValue(first().value), linkedMapOf())
    else -> {
      val exampleValues = mapIndexed { index, example ->
        val name = example.name ?: "${Example::class.java.canonicalName}$index"
        val referenced = Referenced.Other(
          Example(
            summary = example.summary,
            description = null,
            value = exampleValue(example.value),
            externalValue = null
          )
        )
        name to referenced
      }.toMap(linkedMapOf())
      Examples(null, exampleValues)
    }
  }
