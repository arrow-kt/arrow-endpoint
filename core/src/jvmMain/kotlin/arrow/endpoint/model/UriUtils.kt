package arrow.endpoint.model

import java.net.URI

public fun uri(javaUri: URI): Uri? =
  Uri.parse(javaUri.toString()).orNull()

public fun Uri.toJavaUri(): URI = URI(toString())

public fun Uri.resolveOrNull(other: Uri): Uri? =
  uri(toJavaUri().resolve(other.toJavaUri()))
