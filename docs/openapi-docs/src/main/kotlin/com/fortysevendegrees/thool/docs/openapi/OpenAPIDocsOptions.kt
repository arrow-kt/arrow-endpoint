package com.fortysevendegrees.thool.docs.openapi

import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.model.Method

public data class OpenAPIDocsOptions(
  val operationIdGenerator: (List<String>, Method) -> String = Companion::defaultOperationIdGenerator,
  val schemaName: (Schema.ObjectInfo) -> String = Companion::defaultSchemaName
) {
  public companion object {
    public fun defaultOperationIdGenerator(pathComponents: List<String>, method: Method): String {
      val components = if (pathComponents.isEmpty()) listOf("root") else pathComponents
      // converting to camelCase
      return method.value.toLowerCase() + components.joinToString("") { it.toLowerCase().capitalize() }
    }

    public fun defaultSchemaName(info: Schema.ObjectInfo): String {
      val shortName = info.fullName.split('.').last()
      return (shortName + info.typeParameterShortNames.joinToString("_"))
    }
  }
}
