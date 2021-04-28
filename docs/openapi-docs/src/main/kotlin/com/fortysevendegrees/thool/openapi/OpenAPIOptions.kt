package com.fortysevendegrees.thool.openapi

import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.model.Method

public data class OpenAPIDocsOptions(
  val operationIdGenerator: (Pair<List<String>, Method>) -> String,
  val schemaName: (Schema.ObjectInfo) -> String = defaultSchemaName
) {
  public companion object {
    public val defaultOperationIdGenerator: (Pair<List<String>, Method>) -> String =
      { (pathComponents, method) ->
        val components = if (pathComponents.isEmpty()) listOf("root")
        else pathComponents

        //converting to camelCase
        method.value.toLowerCase() + components.joinToString("") { it.toLowerCase().capitalize() }
      }

    public val default: OpenAPIDocsOptions = OpenAPIDocsOptions(defaultOperationIdGenerator)
  }
}

public val defaultSchemaName: (Schema.ObjectInfo) -> String = { info ->
  val shortName = info.fullName.split('.').last()
  (shortName + info.typeParameterShortNames.joinToString("_"))
}
