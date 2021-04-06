package com.fortysevendegrees.tapir.model

/* inline */ class Method private constructor(val value: String) {

  /**
   * An HTTP com.fortysevendegrees.tapir.method is idempotent if an identical request can be made once or several times in a row with the same
   * effect while leaving the server in the same state.
   * @see https://developer.mozilla.org/en-US/docs/Glossary/Idempotent
   */
  fun isIdempotent(m: Method): Boolean =
    idempotent.contains(m)

  /**
   * An HTTP com.fortysevendegrees.tapir.method is safe if it doesn't alter the state of the server.
   * @see https://developer.mozilla.org/en-US/docs/Glossary/safe
   */
  fun isSafe(m: Method): Boolean =
    safe.contains(m)

  companion object {
    val GET: Method = Method("GET")
    val HEAD: Method = Method("HEAD")
    val POST: Method = Method("POST")
    val PUT: Method = Method("PUT")
    val DELETE: Method = Method("DELETE")
    val OPTIONS: Method = Method("OPTIONS")
    val PATCH: Method = Method("PATCH")
    val CONNECT: Method = Method("CONNECT")
    val TRACE: Method = Method("TRACE")

    /**
     * Parse HTTP com.fortysevendegrees.tapir.method by [method] string
     */
    operator fun invoke(method: String): Method {
      return when (method) {
        GET.value -> GET
        POST.value -> POST
        PUT.value -> PUT
        PATCH.value -> PATCH
        DELETE.value -> DELETE
        HEAD.value -> HEAD
        OPTIONS.value -> OPTIONS
        else -> Method(method)
      }
    }

    private val idempotent: Set<Method> =
      setOf(HEAD, TRACE, GET, PUT, OPTIONS, DELETE)

    private val safe: Set<Method> =
      setOf(HEAD, GET, OPTIONS)
  }
}
