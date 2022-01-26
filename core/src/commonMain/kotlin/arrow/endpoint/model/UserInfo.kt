package arrow.endpoint.model

import arrow.endpoint.model.Rfc3986.encode

public data class UserInfo(val username: String, val password: String?) {
  override fun toString(): String =
    "${username.encode(Rfc3986.UserInfo)}${password?.let { ":${it.encode(Rfc3986.UserInfo)}" } ?: ""}"
}
