@file:Suppress("FunctionName")

package arrow.endpoint.model

import java.net.URI

public fun Uri(javaUri: URI): Uri? =
  parseToUri(javaUri.toString()).orNull()

public fun Uri.toJavaUri(): URI = URI(toString())

public fun Uri.resolveOrNull(other: Uri): Uri? =
  Uri(toJavaUri().resolve(other.toJavaUri()))
