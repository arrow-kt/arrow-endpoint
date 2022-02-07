package arrow.endpoint.model

import arrow.endpoint.model.Rfc3986.encode

public data class Authority(
  val userInfo: UserInfo? = null,
  val hostSegment: HostSegment = HostSegment(""),
  val port: Int? = null
) {

  /** Replace the user info with a username only. */
  public fun userInfo(username: String): Authority = this.copy(userInfo = UserInfo(username, null))

  /** Replace the user info with username/password combination. */
  public fun userInfo(username: String, password: String): Authority =
    this.copy(userInfo = UserInfo(username, password))

  /** Replace the user info. */
  public fun userInfo(ui: UserInfo?): Authority = this.copy(userInfo = ui)

  /** Replace the host. Does not validate the new host value if it's nonempty. */
  public fun host(h: String): Authority = this.copy(hostSegment = HostSegment(h))

  public fun host(): String = hostSegment.v

  /** Replace the port. */
  public fun port(p: Int?): Authority = this.copy(port = p)

  override fun toString(): String {
    fun encodeUserInfo(ui: UserInfo): String = buildString {
      if (ui.username.isNotEmpty()) {
        append(ui.username.encode(Rfc3986.UserInfo))
        if (ui.password?.isNotEmpty() == true) {
          append(":${ui.password.encode(Rfc3986.UserInfo)}")
        }
        append("@")
      }
    }

    val userInfoS = userInfo?.let { encodeUserInfo(it) } ?: ""
    val hostS = hostSegment.encoded()
    val portS = port?.let { ":$it" } ?: ""

    return "//$userInfoS$hostS$portS"
  }
}
