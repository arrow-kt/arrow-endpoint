package com.fortysevendegrees.thool.openapi
//private[openapi] class CodecToMediaType(objectSchemas: Schemas) {
//  def apply[T, CF <: CodecFormat](o: Codec[_, T, CF], examples: List[EndpointIO.Example[T]]): ListMap[String, OMediaType] = {
//    val convertedExamples = com.fortysevendegrees.thool.openapi.ExampleConverter.convertExamples(o, examples)
//
//    ListMap(
//      o.format.mediaType.noCharset.toString -> OMediaType(
//    Some(objectSchemas(o)),
//    convertedExamples.singleExample,
//    convertedExamples.multipleExamples,
//    ListMap.empty
//    )
//    )
//  }
//}
