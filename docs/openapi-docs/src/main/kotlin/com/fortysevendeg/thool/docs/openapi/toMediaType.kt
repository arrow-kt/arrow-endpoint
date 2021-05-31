package com.fortysevendeg.thool.docs.openapi

import com.fortysevendeg.thool.Codec
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.Schema

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
