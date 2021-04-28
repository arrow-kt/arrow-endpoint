package com.fortysevendegrees.thool.openapi.schema

import com.fortysevendegrees.thool.openapi.ReferenceOr
import arrow.core.prependTo
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointIO
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.EndpointOutput
import com.fortysevendegrees.thool.FieldName
import com.fortysevendegrees.thool.Schema

fun Iterable<Endpoint<*, *, *>>.toSchemas(
  schemaName: (Schema.ObjectInfo) -> String
): Pair<Map<ObjectKey, ReferenceOr<com.fortysevendegrees.thool.openapi.schema.Schema>>, Schemas> {
  val sObjects =
    flatMap { e -> forInput(e.input) + forOutput(e.errorOutput) + forOutput(e.output) }//.unique()
  val infoToKey = sObjects.map(Pair<Schema.ObjectInfo, Schema<*>>::first).calculateUniqueKeys(schemaName)

  val objectToSchemaReference = ObjectToSchemaReference(infoToKey)
  val schemas = Schemas(objectToSchemaReference)
  val infosToSchema = sObjects.map { td -> Pair(td.first, schemas(td.second)) }.toMap()

  return Pair(infosToSchema.mapKeys { (info, _) -> infoToKey[info]!! }, schemas)
}

private fun forInput(input: EndpointInput<*>): List<Pair<Schema.ObjectInfo, Schema<*>>> =
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

private fun forOutput(output: EndpointOutput<*>): List<Pair<Schema.ObjectInfo, Schema<*>>> =
  when (output) {
    is EndpointOutput.FixedStatusCode -> emptyList()
    is EndpointOutput.StatusCode -> emptyList()
    is EndpointOutput.Void -> emptyList()
    is EndpointOutput.Pair<*, *, *> -> forOutput(output.first) + forOutput(output.second)
    is EndpointOutput.MappedPair<*, *, *, *> -> forOutput(output.output)
    is EndpointIO<*> -> forInput(output)
  }

private fun forIO(io: EndpointIO<*>): List<Pair<Schema.ObjectInfo, Schema<*>>> =
  when (io) {
    is EndpointIO.Body<*, *> -> io.codec.toSchemas()
    is EndpointIO.Header -> io.codec.toSchemas()
    is EndpointIO.Empty -> emptyList()
    is EndpointIO.Pair<*, *, *> -> forIO(io.first) + forIO(io.second)
    is EndpointIO.MappedPair<*, *, *, *> -> forIO(io.wrapped)
  }

private fun <A> Codec<*, A, *>.toSchemas(): List<Pair<Schema.ObjectInfo, Schema<*>>> =
  schema().toSchemas()

private fun <A> Schema<A>.toSchemas(): List<Pair<Schema.ObjectInfo, Schema<*>>> =
  when (this) {
    is Schema.List -> this.element.toSchemas()
    is Schema.Product -> this.toSchemas()
    is Schema.Coproduct -> this.toSchemas()
    is Schema.OpenProduct -> Pair(objectInfo, this) prependTo valueSchema.toSchemas()
    else -> emptyList()
  }

private fun Schema.Coproduct<*>.toSchemas(): List<Pair<Schema.ObjectInfo, Schema<*>>> =
  Pair(objectInfo, this) prependTo subtypesSchemas().flatMap(Schema<*>::toSchemas)

private fun Schema.Product<*>.toSchemas(): List<Pair<Schema.ObjectInfo, Schema<*>>> =
  Pair(objectInfo, this) prependTo fieldsSchema().flatMap(Schema<*>::toSchemas)

private fun Schema.Product<*>.fieldsSchema(): List<Schema<*>> =
  fields.map(Pair<FieldName, Schema<*>>::second)

private fun Schema.Coproduct<*>.subtypesSchemas(): List<Schema<*>> =
  schemas.mapNotNull { (it as? Schema.Product<*>) }
