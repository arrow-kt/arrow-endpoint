package com.fortysevendegrees.thool.openapi

import com.fortysevendegrees.thool.openapi.schema.Schema
import kotlinx.serialization.Serializable

//typealias SecurityRequirement = Map<String, List<String>>
//
//data class SecurityScheme(
//  val type: String,
//  val description: String?,
//  val name: String?,
//  val input: String?,
//  val scheme: String?,
//  val bearerFormat: String?,
//  val flows: OAuthFlows?,
//  val openIdConnectUrl: String?
//)

//data class OAuthFlows(
//  val implicit: OAuthFlow? = null,
//  val password: OAuthFlow? = null,
//  val clientCredentials: OAuthFlow? = null,
//  val authorizationCode: OAuthFlow? = null
//)

//data class OAuthFlow(
//  val authorizationUrl: String,
//  val tokenUrl: String?,
//  val refreshUrl: String?,
//  val scopes: Map<String, String>
//)

data class OpenAPI(
  val openapi: String = "3.0.3",
  val info: Info,
  val tags: List<Tag>,
  val servers: List<Server>,
  val paths: Map<String, PathItem>,
  val components: Components?,
//  val security: List<SecurityRequirement>
) {
  fun addPathItem(path: String, pathItem: PathItem): OpenAPI {
    val pathItem2 = when (val existing = paths[path]) {
      null -> pathItem
      else -> existing.mergeWith(pathItem)
    }

    return copy(paths = paths + Pair(path, pathItem2))
  }

  fun servers(s: List<Server>): OpenAPI = copy(servers = s)

  fun tags(t: List<Tag>): OpenAPI = copy(tags = t)
}

@Serializable
data class Info(
  val title: String,
  val version: String,
  val description: String? = null,
  val termsOfService: String? = null,
  val contact: Contact? = null,
  val license: License? = null,
)

@Serializable
data class Contact(val name: String?, val email: String?, val url: String?)
@Serializable
data class License(val name: String, val url: String?)

@Serializable
data class Server(
  val url: String,
  val description: String? = null,
  val variables: Map<String, ServerVariable>? = null
) {
  fun description(d: String): Server = copy(description = d)
  fun variables(vararg vars: Pair<String, ServerVariable>): Server = copy(variables = vars.toMap())
}

@Serializable
data class ServerVariable(val enum: List<String>?, val default: String, val description: String?) {
  init {
    require(enum?.contains(default) ?: true) {
      "ServerVariable#default must be one of the values in enum if enum is defined"
    }
  }
}

// todo: responses, parameters, examples, requestBodies, headers, links, callbacks
data class Components(
  val schemas: Map<String, ReferenceOr<Schema>>,
//  val securitySchemes: Map<String, ReferenceOr<SecurityScheme>>
)

// todo: $ref
data class PathItem(
  val summary: String?,
  val description: String?,
  val get: Operation?,
  val put: Operation?,
  val post: Operation?,
  val delete: Operation?,
  val options: Operation?,
  val head: Operation?,
  val patch: Operation?,
  val trace: Operation?,
  val servers: List<Server>,
  val parameters: List<ReferenceOr<Parameter>>
) {
  fun mergeWith(other: PathItem): PathItem =
    PathItem(
      null,
      null,
      get = get ?: other.get,
      put = put ?: other.put,
      post = post ?: other.post,
      delete = delete ?: other.delete,
      options = options ?: other.options,
      head = head ?: other.head,
      patch = patch ?: other.patch,
      trace = trace ?: other.trace,
      servers = emptyList(),
      parameters = emptyList()
    )
}

// todo: external docs, callbacks, security
data class Operation(
  val tags: List<String>,
  val summary: String?,
  val description: String?,
  val operationId: String,
  val parameters: List<ReferenceOr<Parameter>>,
  val requestBody: ReferenceOr<RequestBody>?,
  val responses: Map<ResponsesKey, ReferenceOr<Response>>,
  val deprecated: Boolean?,
//  val security: List<SecurityRequirement>,
  val servers: List<Server>
)

data class Parameter(
  val name: String,
  val input: ParameterIn,
  val description: String?,
  val required: Boolean?,
  val deprecated: Boolean?,
  val allowEmptyValue: Boolean?,
  val style: ParameterStyle?,
  val explode: Boolean?,
  val allowReserved: Boolean?,
  val schema: ReferenceOr<Schema>,
  val example: ExampleValue?,
  val examples: Map<String, ReferenceOr<Example>>,
  val content: Map<String, MediaType>
)

@Serializable
enum class ParameterIn { Query, Header, Path, Cookie; }

@Serializable
enum class ParameterStyle {
  Simple,
  Form,
  Matrix,
  Label,
  SpaceDelimited,
  PipeDelimited,
  DeepObject;
}

data class RequestBody(val description: String?, val content: Map<String, MediaType>, val required: Boolean?)

data class MediaType(
  val schema: ReferenceOr<Schema>?,
  val example: ExampleValue?,
  val examples: Map<String, ReferenceOr<Example>>,
  val encoding: Map<String, Encoding>
)

data class Encoding(
  val contentType: String?,
  val headers: Map<String, ReferenceOr<Header>>,
  val style: ParameterStyle?,
  val explode: Boolean?,
  val allowReserved: Boolean?
)

sealed interface ResponsesKey
@Serializable
object ResponsesDefaultKey : ResponsesKey
@Serializable
inline class ResponsesCodeKey(val code: Int) : ResponsesKey

// todo: links
data class Response(
  val description: String,
  val headers: Map<String, ReferenceOr<Header>>,
  val content: Map<String, MediaType>
) {

  fun merge(other: Response): Response =
    Response(description, headers + other.headers, content + other.content)
}

@Serializable
data class Example(
  val summary: String?,
  val description: String?,
  val value: ExampleValue?,
  val externalValue: String?
)

data class Header(
  val description: String?,
  val required: Boolean?,
  val deprecated: Boolean?,
  val allowEmptyValue: Boolean?,
  val style: ParameterStyle?,
  val explode: Boolean?,
  val allowReserved: Boolean?,
  val schema: ReferenceOr<Schema>?,
  val example: ExampleValue?,
  val examples: Map<String, ReferenceOr<Example>>,
  val content: Map<String, MediaType>
)
