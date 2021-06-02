package com.fortysevendeg.thool.docs.openapi

import com.fortysevendeg.thool.Schema
import com.fortysevendeg.thool.model.Method

public data class OpenAPIDocsOptions(
  val operationIdGenerator: (List<String>, Method) -> String = Companion::defaultOperationIdGenerator,
  val schemaName: (Schema.ObjectInfo) -> String = Companion::defaultSchemaName
) {
  public companion object {
    public fun defaultOperationIdGenerator(pathComponents: List<String>, method: Method): String {
      val components = pathComponents.ifEmpty { listOf("root") }
      // converting to camelCase
      return method.value.lowercase() + components.joinToString("") { it.lowercase().capitalize() }
    }

    public fun defaultSchemaName(info: Schema.ObjectInfo): String {
      val shortName = info.fullName.split('.').last()
      return (shortName + info.typeParameterShortNames.joinToString("_"))
    }
  }
}
