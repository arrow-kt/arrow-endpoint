package com.fortysevendegrees.thool.model

public interface ResponseMetadata : HasHeaders {
  val code: StatusCode
  val statusText: String

  val is200: Boolean
    get() = code == StatusCode.Ok

  val isSuccess: Boolean
    get() = code.isSuccess()

  val isRedirect: Boolean
    get() = code.isRedirect()

  val isClientError: Boolean
    get() = code.isClientError()

  val isServerError: Boolean
    get() = code.isServerError()

  public companion object {
    operator fun invoke(statusCode: StatusCode, _statusText: String, _headers: List<Header>): ResponseMetadata =
      object : ResponseMetadata {
        override val headers: List<Header> = _headers
        override val code: StatusCode = statusCode
        override val statusText: String = _statusText

        override fun toString(): String = "ResponseMetadata($code,$statusText,${headers.toStringSafe()})"
      }
  }
}
