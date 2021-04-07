package com.fortysevendegrees.thool

data class FieldName(val name: String, val encodedName: String) {
  constructor(name: String) : this(name, name)
}
