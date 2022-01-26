package arrow.endpoint.model

import kotlin.jvm.JvmInline

public sealed interface UriError {
  @JvmInline
  public value class UnexpectedScheme(public val errorMessage: String) : UriError

  @JvmInline
  public value class CantParse(public val errorMessage: String) : UriError
  public object InvalidHost : UriError
  public object InvalidPort : UriError

  @JvmInline
  public value class IllegalArgument(public val errorMessage: String) : UriError
}
