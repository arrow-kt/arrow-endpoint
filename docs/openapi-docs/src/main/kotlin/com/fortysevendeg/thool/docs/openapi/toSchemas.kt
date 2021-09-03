package com.fortysevendeg.thool.docs.openapi

import arrow.core.prependTo
import com.fortysevendeg.thool.Codec
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.EndpointInput
import com.fortysevendeg.thool.EndpointOutput
import com.fortysevendeg.thool.FieldName
import com.fortysevendeg.thool.Schema as TSchema

public fun Iterable<Endpoint<*, *, *>>.toSchemas(
  schemaName: (TSchema.ObjectInfo) -> String
): Pair<Map<String, Referenced<Schema>>, Map<TSchema.ObjectInfo, String>> {
  val sObjects: List<Pair<TSchema.ObjectInfo, TSchema<*>>> =
    flatMap { e -> forInput(e.input) + forOutput(e.errorOutput) + forOutput(e.output) }.unique()

  val infoToKey: Map<TSchema.ObjectInfo, String> =
    sObjects.map(Pair<TSchema.ObjectInfo, TSchema<*>>::first).calculateUniqueKeys(schemaName)

  val infosToSchema: Map<TSchema.ObjectInfo, Referenced<Schema>> =
    sObjects.associateTo(linkedMapOf()) { (info, schema) -> Pair(info, infoToKey._referenceOrSchema(schema)) }

  val schemas: Map<String, Referenced<Schema>> =
    infosToSchema.mapKeysTo(linkedMapOf()) { (info, _) -> infoToKey[info]!! }

  return Pair(schemas, infoToKey)
}

private fun forInput(input: EndpointInput<*>): List<Pair<TSchema.ObjectInfo, TSchema<*>>> =
  when (input) {
    is EndpointInput.Cookie -> input.codec.toSchemas()
    is EndpointInput.PathCapture -> input.codec.toSchemas()
    is EndpointInput.Query -> input.codec.toSchemas()
    is EndpointInput.FixedMethod -> emptyList()
    is EndpointInput.FixedPath -> emptyList()
    is EndpointInput.PathsCapture -> emptyList()
    is EndpointInput.QueryParams -> emptyList()
    is EndpointInput.Pair<*, *, *> -> forInput(input.first) + forInput(input.second)
    is EndpointInput.MappedPair<*, *, *, *> -> forInput(input.input)
    is EndpointIO -> forIO(input)
  }

private fun forOutput(output: EndpointOutput<*>): List<Pair<TSchema.ObjectInfo, TSchema<*>>> =
  when (output) {
    is EndpointOutput.FixedStatusCode -> emptyList()
    is EndpointOutput.StatusCode -> emptyList()
    is EndpointOutput.Void -> emptyList()
    is EndpointOutput.OneOf<*, *> -> output.mappings.flatMap { mapping -> forOutput(mapping.output) }
    is EndpointOutput.Pair<*, *, *> -> forOutput(output.first) + forOutput(output.second)
    is EndpointOutput.MappedPair<*, *, *, *> -> forOutput(output.output)
    is EndpointIO<*> -> forInput(output)
  }

private fun forIO(io: EndpointIO<*>): List<Pair<TSchema.ObjectInfo, TSchema<*>>> =
  when (io) {
    is EndpointIO.Body<*, *> -> io.codec.toSchemas()
    is EndpointIO.Header -> io.codec.toSchemas()
    is EndpointIO.Empty -> emptyList()
    is EndpointIO.Pair<*, *, *> -> forIO(io.first) + forIO(io.second)
    is EndpointIO.MappedPair<*, *, *, *> -> forIO(io.wrapped)
  }

private fun <A> Codec<*, A, *>.toSchemas(): List<Pair<TSchema.ObjectInfo, TSchema<*>>> =
  schema().toSchemas()

private fun <A> TSchema<A>.toSchemas(): List<Pair<TSchema.ObjectInfo, TSchema<*>>> =
  when (this) {
    is TSchema.List -> this.element.toSchemas()
    is TSchema.Product -> this.toSchemas()
    is TSchema.Coproduct -> this.toSchemas()
    is TSchema.OpenProduct -> Pair(objectInfo, this) prependTo valueSchema.toSchemas()
    else -> emptyList()
  }

private fun TSchema.Coproduct<*>.toSchemas(): List<Pair<TSchema.ObjectInfo, TSchema<*>>> =
  Pair(objectInfo, this) prependTo subtypesSchemas().flatMap(TSchema<*>::toSchemas)

private fun TSchema.Product<*>.toSchemas(): List<Pair<TSchema.ObjectInfo, TSchema<*>>> =
  Pair(objectInfo, this) prependTo fieldsSchema().flatMap(TSchema<*>::toSchemas)

private fun TSchema.Product<*>.fieldsSchema(): List<TSchema<*>> =
  fields.map(Pair<FieldName, TSchema<*>>::second)

private fun TSchema.Coproduct<*>.subtypesSchemas(): List<TSchema<*>> =
  schemas.mapNotNull { (it as? TSchema.Product<*>) }
