package arrow.endpoint.model

public value class Method private constructor(public val value: String) {

  /**
   * An HTTP method is idempotent if an identical request can be made once or several times in a row with the same
   * effect while leaving the server in the same state.
   * @link https://developer.mozilla.org/en-US/docs/Glossary/Idempotent
   */
  public fun isIdempotent(m: Method): Boolean =
    idempotent.contains(m)

  /**
   * An HTTP method is safe if it doesn't alter the state of the server.
   * @link https://developer.mozilla.org/en-US/docs/Glossary/safe
   */
  public fun isSafe(m: Method): Boolean =
    safe.contains(m)

  public companion object {
    public val GET: Method = Method("GET")
    public val HEAD: Method = Method("HEAD")
    public val POST: Method = Method("POST")
    public val PUT: Method = Method("PUT")
    public val DELETE: Method = Method("DELETE")
    public val OPTIONS: Method = Method("OPTIONS")
    public val PATCH: Method = Method("PATCH")
    public val CONNECT: Method = Method("CONNECT")
    public val TRACE: Method = Method("TRACE")

    /**
     * Parse HTTP method by [method] string
     */
    public operator fun invoke(method: String): Method {
      return when (method) {
        GET.value -> GET
        POST.value -> POST
        PUT.value -> PUT
        PATCH.value -> PATCH
        DELETE.value -> DELETE
        HEAD.value -> HEAD
        OPTIONS.value -> OPTIONS
        CONNECT.value -> CONNECT
        TRACE.value -> TRACE
        else -> Method(method)
      }
    }

    private val idempotent: Set<Method> =
      setOf(HEAD, TRACE, GET, PUT, OPTIONS, DELETE)

    private val safe: Set<Method> =
      setOf(HEAD, GET, OPTIONS)
  }

  override fun toString(): String = "Method($value)"
}
