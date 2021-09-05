package arrow.endpoint

public data class FieldName(val name: String, val encodedName: String) {
  public constructor(name: String) : this(name, name)
}
