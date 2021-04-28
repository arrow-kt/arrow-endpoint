package com.fortysevendegrees.thool.openapi

import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.openapi.schema.toSchemas

fun Iterable<Endpoint<*, *, *>>.toOpenAPI(
  info: Info,
  options: OpenAPIDocsOptions
): OpenAPI {
  // Rename all EndpointInput.Path to have a name, ignore Endpoints that have websSockets
  val (keyToSchema, schemas) = this.toSchemas(options.schemaName)
//  val securitySchemes = SecuritySchemesForEndpoints(es2)



  TODO()
}
