package arrow.endpoint.docs.openapi

import arrow.endpoint.Codec
import arrow.endpoint.EndpointIO
import arrow.endpoint.Schema

public fun Codec<*, *, *>.toMediaTypeMap(
  schemas: Map<Schema.ObjectInfo, String>,
  endpointExamples: List<EndpointIO.Info.Example<Any?>>
): Map<String, MediaType> {
  @Suppress("UNCHECKED_CAST")
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
