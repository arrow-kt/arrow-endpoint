package com.fortysevendeg.thool.docs.openapi

import com.fortysevendeg.thool.Endpoint

public fun <I, E, O> Endpoint<I, E, O>.toOpenAPI(
  title: String,
  version: String,
  options: OpenAPIDocsOptions = OpenAPIDocsOptions()
): OpenApi =
  toOpenAPI(Info(title, version = version), options)

public fun <I, E, O> Endpoint<I, E, O>.toOpenAPI(info: Info, options: OpenAPIDocsOptions = OpenAPIDocsOptions()): OpenApi =
  listOf(this).toOpenAPI(info, options)

public fun Iterable<Endpoint<*, *, *>>.toOpenAPI(
  title: String,
  version: String,
  options: OpenAPIDocsOptions = OpenAPIDocsOptions()
): OpenApi =
  toOpenAPI(Info(title, version = version), options)

public fun Iterable<Endpoint<*, *, *>>.toOpenAPI(
  info: Info,
  options: OpenAPIDocsOptions = OpenAPIDocsOptions()
): OpenApi {
  val (keyToSchema, schemas) = toSchemas(options.schemaName)
  val securitySchemes = toSecuritySchemes()
  val pathCreator = PathCreator(schemas, securitySchemes, options)

  val openApi = OpenApi(
    info = info,
    servers = emptyList(),
    paths = linkedMapOf(),
    components = keyToSchema.toComponents(securitySchemes),
    security = emptyList(),
    tags = LinkedHashSet()
  )

  return map(pathCreator::pathItem)
    .fold(openApi) { acc, (path, pathItem) ->
      acc.addPathItem(path, pathItem)
    }
}

private fun Map<String, Referenced<Schema>>.toComponents(securitySchemes: SecuritySchemes): Components? =
  if (this.isNotEmpty() || securitySchemes.isNotEmpty()) Components(
    this,
    securitySchemes = securitySchemes.values.toMap().mapValues { (_, scheme) -> Referenced.Other(scheme) }.toMap()
  )
  else null
