package com.fortysevendegrees.thool

public data class FieldName(val name: String, val encodedName: String) {
  constructor(name: String) : this(name, name)
}
