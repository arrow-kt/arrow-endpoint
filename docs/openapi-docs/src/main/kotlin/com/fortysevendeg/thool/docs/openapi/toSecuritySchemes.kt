package com.fortysevendeg.thool.docs.openapi

import arrow.core.tail
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.EndpointIO
import com.fortysevendeg.thool.EndpointInput
import kotlinx.serialization.Serializable

@Serializable
public data class SecurityScheme(
  val type: String,
  val description: String? = null,
  val name: String? = null,
  val input: String? = null,
  val scheme: String? = null,
  val bearerFormat: String? = null,
  val flows: OAuthFlows? = null,
  val openIdConnectUrl: String? = null,
)

@Serializable
public data class OAuthFlows(
  val value: OAuthFlow? = null,
  val password: OAuthFlow? = null,
  val clientCredentials: OAuthFlow? = null,
  val authorizationCode: OAuthFlow? = null
)

@Serializable
public data class OAuthFlow(
  val authorizationUrl: String,
  val tokenUrl: String? = null,
  val refreshUrl: String? = null,
  val scopes: LinkedHashMap<String, String>
)

public typealias SecuritySchemes = Map<EndpointInput.Auth<*>, Pair<String, SecurityScheme>>

public fun Iterable<Endpoint<*, *, *>>.toSecuritySchemes(): SecuritySchemes {
  val auths = flatMap { e -> e.input.auths() }
  val authSecuritySchemes = auths.map { a -> Pair(a, authToSecurityScheme(a)) }
  val securitySchemes = authSecuritySchemes.map { (auth, scheme) -> Pair(auth.securitySchemeName, scheme) }.toSet()
  val takenNames = authSecuritySchemes.mapNotNull { (a, _) -> a.securitySchemeName }.toSet()
  val namedSecuritySchemes = nameSecuritySchemes(securitySchemes.toList(), takenNames, linkedMapOf())
  return authSecuritySchemes.map { (a, s) ->
    Pair(
      a,
      Pair( // TODO Add library exception
        namedSecuritySchemes.getOrElse(s) { throw RuntimeException("Internal exception") },
        s
      )
    )
  }.toMap()
}

private tailrec fun nameSecuritySchemes(
  schemes: List<Pair<String?, SecurityScheme>>,
  takenNames: Set<String>,
  acc: Map<SecurityScheme, String>
): Map<SecurityScheme, String> {
  val first = schemes.firstOrNull()
  val tail = schemes.tail()
  return if (first == null) acc
  else {
    val (name, scheme) = first
    if (name != null) nameSecuritySchemes(tail, takenNames, acc + Pair(scheme, name))
    else {
      val baseName = scheme.type + "Auth"
      val name = uniqueName(baseName) { it !in takenNames }
      nameSecuritySchemes(tail, takenNames + name, acc + Pair(scheme, name))
    }
  }
}

private fun authToSecurityScheme(a: EndpointInput.Auth<*>): SecurityScheme =
  when (a) {
    is EndpointInput.Auth.ApiKey -> {
      val (name, input) = apiKeyInputNameAndIn(a.input.asListOfBasicInputs())
      SecurityScheme("apiKey", null, name, input)
    }
  }

private fun apiKeyInputNameAndIn(input: List<EndpointInput.Basic<*, *, *>>) =
  when (val res = input.firstOrNull()) {
    is EndpointIO.Header -> Pair(res.name, "header")
    is EndpointInput.Cookie -> Pair(res.name, "query")
    is EndpointInput.Query -> Pair(res.name, "cookie")
    else -> throw IllegalArgumentException("Api key authentication can only be read from headers, queries or cookies, not: $input")
  }
