package com.fortysevendegrees.thool.openapi

import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.openapi.schema.Schema

internal object EndpointInputToParameterConverter {
  fun <T> from(query: EndpointInput.Query<T>, schema: ReferenceOr<Schema>): Parameter {
    val examples = ExampleConverter.convertExamples(query.codec, query.info.examples)

    return Parameter(
      query.name,
      ParameterIn.Query,
      query.info.description,
      !query.codec.schema().isOptional(),
      if (query.info.deprecated) true else null,
      null,
      null,
      null,
      null,
      schema,
      examples.singleExample,
      examples.multipleExamples,
      emptyMap()
    )
  }

  fun <T> from(pathCapture: EndpointInput.PathCapture<T>, schema: ReferenceOr<Schema>): Parameter {
    val examples = ExampleConverter.convertExamples(pathCapture.codec, pathCapture.info.examples)
    return Parameter(
      pathCapture.name ?: "?",
      ParameterIn.Path,
      pathCapture.info.description,
      true,
      null,
      null,
      null,
      null,
      null,
      schema,
      examples.singleExample,
      examples.multipleExamples,
      emptyMap()
    )
  }

  fun <T> from(headers: EndpointIO.Header<T>, schema: ReferenceOr<Schema>): Parameter {
    val examples = ExampleConverter.convertExamples(headers.codec, headers.info.examples)
    return Parameter(
      headers.name,
      ParameterIn.Header,
      headers.info.description,
      !headers.codec.schema().isOptional(),
      if (headers.info.deprecated) true else null,
      null,
      null,
      null,
      null,
      schema,
      examples.singleExample,
      examples.multipleExamples,
      emptyMap()
    )
  }

  fun <T> from(cookie: EndpointInput.Cookie<T>, schema: ReferenceOr<Schema>): Parameter {
    val examples = ExampleConverter.convertExamples(cookie.codec, cookie.info.examples)
    return Parameter(
      cookie.name,
      ParameterIn.Cookie,
      cookie.info.description,
      !cookie.codec.schema().isOptional(),
      if (cookie.info.deprecated) true else null,
      null,
      null,
      null,
      null,
      schema,
      examples.singleExample,
      examples.multipleExamples,
      emptyMap()
    )
  }
}
