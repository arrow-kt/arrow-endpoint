package com.fortysevendegrees.thool.docs.openapi

import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput

public fun <T> EndpointInput.Query<T>.toParameter(schema: Referenced<Schema>): Parameter {
  val examples = info.examples.toExamples(codec)
  return Parameter(
    name,
    ParameterIn.query,
    info.description,
    !codec.schema().isOptional(),
    info.deprecated,
    schema = schema,
    example = examples.singleExample,
    examples = examples.multipleExamples
  )
}

public fun <T> EndpointInput.PathCapture<T>.toParameter(schema: Referenced<Schema>): Parameter {
  val examples = info.examples.toExamples(codec)
  return Parameter(
    name ?: "?",
    ParameterIn.path,
    info.description,
    true,
    schema = schema,
    example = examples.singleExample,
    examples = examples.multipleExamples,
  )
}

public fun <T> EndpointIO.Header<T>.toParameter(schema: Referenced<Schema>): Parameter {
  val examples = info.examples.toExamples(codec)
  return Parameter(
    name,
    ParameterIn.header,
    info.description,
    !codec.schema().isOptional(),
    info.deprecated,
    schema = schema,
    example = examples.singleExample,
    examples = examples.multipleExamples,
  )
}

public fun <T> EndpointInput.Cookie<T>.toParameter(schema: Referenced<Schema>): Parameter {
  val examples = info.examples.toExamples(codec)
  return Parameter(
    name,
    ParameterIn.cookie,
    info.description,
    !codec.schema().isOptional(),
    info.deprecated,
    schema = schema,
    example = examples.singleExample,
    examples = examples.multipleExamples,
  )
}
