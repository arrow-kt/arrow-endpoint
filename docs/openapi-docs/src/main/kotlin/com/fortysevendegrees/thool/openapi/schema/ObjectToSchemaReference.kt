package com.fortysevendegrees.thool.openapi.schema

import com.fortysevendegrees.thool.Schema
import com.fortysevendegrees.thool.openapi.Reference

// TODO replace by extension functiosn over Map
class ObjectToSchemaReference(val infoToKey: Map<Schema.ObjectInfo, ObjectKey>) {
  fun map(objectInfo: Schema.ObjectInfo): Reference =
    Reference.to(
      "#/components/schemas/",
      infoToKey[objectInfo] ?: throw NoSuchElementException("key not found: $objectInfo")
    )
}
