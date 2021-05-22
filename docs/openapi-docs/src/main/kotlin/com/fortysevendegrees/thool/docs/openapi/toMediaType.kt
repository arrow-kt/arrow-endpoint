package com.fortysevendegrees.thool.docs.openapi

import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.Schema

public fun Codec<*, *, *>.toMediaTypeMap(
  schemas: Map<Schema.ObjectInfo, String>,
  endpointExamples: List<EndpointIO.Info.Example<Any?>>
): Map<String, MediaType> {
  val examples = endpointExamples.toExamples(this as Codec<*, Any?, *>)
  return linkedMapOf(
    format.mediaType.noCharset().toString() to MediaType(
      schemas.referenceOr(this),
      examples.singleExample,
      examples.multipleExamples,
      emptyMap()
    )
  )
}
