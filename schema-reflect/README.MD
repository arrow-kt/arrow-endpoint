# Kotlin Reflect derivation

Semi-auto derivation for Schema.

By using `Schema.reflect` you can retrieve a `Schema` for any `data class`, `sealed class` or `enum` or any mixture of these types.
There is no support for generics, since these get erased at runtime and thus are not available for reflection.
This means Collections, or Tuples are not supported.