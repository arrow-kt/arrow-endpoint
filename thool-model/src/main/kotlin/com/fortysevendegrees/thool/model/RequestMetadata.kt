package com.fortysevendegrees.thool.model

public interface RequestMetadata : Headers {
  public val method: Method
  public val uri: Uri

  public companion object {
    public operator fun invoke(method: Method, uri: Uri, headers: List<Header>): RequestMetadata =
      RequestMetadataImpl(method, uri, headers)
  }
}

private data class RequestMetadataImpl(
  override val method: Method,
  override val uri: Uri,
  override val headers: List<Header>
) : RequestMetadata {
  override fun toString(): String = "RequestMetadata($method,$uri,${headers.toStringSafe()})"
}
