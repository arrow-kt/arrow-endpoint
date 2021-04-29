package com.fortysevendegrees.thool.model

public interface ResponseMetadata : Headers {
  public val code: StatusCode
  public val statusText: String

  public val is200: Boolean
    get() = code == StatusCode.Ok

  public val isSuccess: Boolean
    get() = code.isSuccess()

  public val isRedirect: Boolean
    get() = code.isRedirect()

  public val isClientError: Boolean
    get() = code.isClientError()

  public val isServerError: Boolean
    get() = code.isServerError()

  public companion object {
    public operator fun invoke(statusCode: StatusCode, statusText: String, headers: List<Header>): ResponseMetadata =
      ResponseMetadataImpl(statusCode, statusText, headers)
  }
}

private data class ResponseMetadataImpl(
  override val code: StatusCode,
  override val statusText: String,
  override val headers: List<Header>
) : ResponseMetadata {
  override fun toString(): String = "ResponseMetadata($code,$statusText,${headers.toStringSafe()})"
}
