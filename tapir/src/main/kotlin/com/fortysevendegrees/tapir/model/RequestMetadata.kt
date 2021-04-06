package com.fortysevendegrees.tapir.model

interface RequestMetadata : HasHeaders {
  val method: Method
  val uri: Uri

  companion object {
    operator fun invoke(_method: Method, _uri: Uri, _headers: List<Header>): RequestMetadata =
      object : RequestMetadata {
        override val headers: List<Header> = _headers
        override val method: Method = _method
        override val uri: Uri = _uri

        override fun toString(): String =
          "RequestMetadata($method,$uri,${headers.toStringSafe()})"
      }
  }
}
