package com.fortysevendegrees.thool.openapi

import arrow.core.Either
import arrow.core.Option
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Schema

public typealias ReferenceOr<T> = Either<Reference, T>

public data class Reference(val ref: String) {
  public companion object {
    public fun to(prefix: String, ref: String): Reference = Reference("$prefix$ref")
  }
}

public sealed interface ExampleValue {
  public companion object {
    public operator fun invoke(v: String): ExampleValue = ExampleSingleValue(v)
    public operator fun <T> invoke(codec: Codec<*, T, *>, e: T): ExampleValue? =
      invoke<T>(codec.schema(), codec.encode(e))

    public operator fun <T> invoke(schema: Schema<*>, raw: Any?): ExampleValue? =
      when (raw) {
        is Iterable<*> -> when (schema) {
          is Schema.List -> ExampleMultipleValue(raw.map(Any?::toString))
          else -> raw.firstOrNull()?.let { ExampleSingleValue(it.toString()) }
        }
        is Option<*> -> raw.fold({ null }) { ExampleSingleValue(it.toString()) }
        null -> null
        else -> ExampleSingleValue(raw.toString())
      }
  }
}

public inline class ExampleSingleValue(val value: String) : ExampleValue
public inline class ExampleMultipleValue(val values: List<String>) : ExampleValue
public data class Tag(val name: String, val description: String? = null, val externalDocs: ExternalDocumentation?)
public data class ExternalDocumentation(val url: String, val description: String?)
