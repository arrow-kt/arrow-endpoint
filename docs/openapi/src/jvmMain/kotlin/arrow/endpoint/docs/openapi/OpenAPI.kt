@file:UseSerializers(
  ReferencedSerializer::class,
  BigDecimalAsStringSerializer::class,
  NelSerializer::class,
  StatusCodeAsIntSerializer::class,
)

package arrow.endpoint.docs.openapi

import arrow.core.NonEmptyList
import arrow.core.Option
import arrow.endpoint.Codec
import arrow.endpoint.model.StatusCode
import java.math.BigDecimal
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A list of definitions that can be used in references. */
public typealias Definitions<A> = Map<String, Referenced<A>>

/**
 * Lists the required security schemes to execute this operation. The object can have multiple
 * security schemes declared in it which are all required (that is, there is a logical AND between
 * the schemes).
 */
public typealias SecurityRequirement = Map<String, List<String>>

private val json: Json = Json {
  encodeDefaults = false
  prettyPrint = true
}

/** This is the root document object for the API specification. */
@Serializable
public data class OpenApi(
  /** Provides metadata about the API. The metadata can be used by the clients if needed. */
  public val info: Info,
  /**
   * An array of Server Objects, which provide connectivity information to a target server. If the
   * servers property is not provided, or is an empty array, the default value would be a 'Server'
   * object with a url value of @/@.
   */
  public val servers: List<Server>,
  /** The available paths and operations for the API. */
  public val paths: Map<String, PathItem>,
  /** An element to hold various schemas for the specification. */
  public val components: Components? = null,
  /**
   * A declaration of which security mechanisms can be used across the API. The list of values
   * includes alternative security requirement objects that can be used. Only one of the security
   * requirement objects need to be satisfied to authorize a request. Individual operations can
   * override this definition. To make security optional, an empty security requirement can be
   * included in the array.
   */
  public val security: List<SecurityRequirement>,
  /**
   * A list of tags used by the specification with additional metadata. The order of the tags can be
   * used to reflect on their order by the parsing tools. Not all tags that are used by the
   * 'Operation' Object must be declared. The tags that are not declared MAY be organized randomly
   * or based on the tools' logic. Each tag name in the list MUST be unique.
   */
  public val tags: LinkedHashSet<Tag>,
  /** Additional external documentation. */
  public val externalDocs: ExternalDocs? = null,
) {
  /** OpenAPI Version used */
  @Suppress("JoinDeclarationAndAssignment") public val openapi: String

  // If assigned immediately, then KotlinX Serialization will not include it in Json definition
  // unless we `encodeDefaults = true`
  // But then all `null` values also get included which results in long Json for small examples.
  init {
    openapi = "3.0.3"
  }

  public fun addPathItem(path: String, pathItem: PathItem): OpenApi {
    val pathItem2 =
      when (val existing = paths[path]) {
        null -> pathItem
        else -> existing.mergeWith(pathItem)
      }

    return copy(paths = paths + Pair(path, pathItem2))
  }

  public fun servers(s: List<Server>): OpenApi = copy(servers = s)

  public fun tags(t: LinkedHashSet<Tag>): OpenApi = copy(tags = t)

  @OptIn(ExperimentalSerializationApi::class)
  public fun toJson(): String = json.encodeToString(this)
}

/**
 * The object provides metadata about the API. The metadata MAY be used by the clients if needed,
 * and MAY be presented in editing or documentation generation tools for convenience.
 */
@Serializable
public data class Info(
  /** The title of the API. */
  public val title: String,
  /**
   * A short description of the API. [CommonMark syntax](https://spec.commonmark.org/) MAY be used
   * for rich text representation.
   */
  public val infoDescription: String? = null,
  /** A URL to the Terms of Service for the API. MUST be in the format of a URL. */
  public val termsOfService: String? = null,
  /** The contact information for the exposed API. */
  public val contact: Contact? = null,
  /** The license information for the exposed API. */
  public val license: License? = null,
  /**
   * The version of the OpenAPI document (which is distinct from the OpenAPI Specification version
   * or the API implementation version).
   */
  public val version: String
)

/** An object representing a Server. */
@Serializable
public data class Server(
  /**
   * A URL to the target host. This URL supports Server Variables and MAY be relative, to indicate
   * that the host location is relative to the location where the OpenAPI document is being served.
   * Variable substitutions will be made when a variable is named in {brackets}.
   */
  public val url: String,
  /**
   * An optional string describing the host designated by the URL. CommonMark syntax MAY be used for
   * rich text representation.
   */
  public val description: String? = null,
  /**
   * A map between a variable name and its value. The value is used for substitution in the server's
   * URL template.
   */
  public val variables: Map<String, ServerVariable>? = null
)

/** An object representing a Server Variable for server URL template substitution. */
@Serializable
public data class ServerVariable(
  /**
   * An enumeration of string values to be used if the substitution options are from a limited set.
   */
  public val enum: NonEmptyList<String>? = null,
  /**
   * The default value to use for substitution, which SHALL be sent if an alternate value is not
   * supplied. Note this behavior is different than the Schema Object's treatment of default values,
   * because in those cases parameter values are optional. If the enum is defined, the value SHOULD
   * exist in the enum's values.
   */
  public val default: String,
  /**
   * An optional description for the server variable. CommonMark syntax MAY be used for rich text
   * representation.
   */
  public val description: String? = null
)

/**
 * Allows adding meta data to a single tag that is used by @Operation@. It is not mandatory to have
 * a @Tag@ per tag used there.
 */
@Serializable
public data class Tag(
  /** The name of the tag. */
  public val name: String,
  /**
   * A short description for the tag. [CommonMark syntax](https://spec.commonmark.org/) MAY be used
   * for rich text representation.
   */
  public val description: String? = null,
  /** Additional external documentation for this tag. */
  public val externalDocs: ExternalDocs? = null
)

/**
 * Holds a set of reusable objects for different aspects of the OAS. All objects defined within the
 * components object will have no effect on the API unless they are explicitly referenced from
 * properties outside the components object.
 */
@Serializable
public data class Components(
  public val schemas: Definitions<Schema> = emptyMap(),
  public val responses: Definitions<Response> = emptyMap(),
  public val parameters: Definitions<Parameter> = emptyMap(),
  public val examples: Definitions<Example> = emptyMap(),
  public val requestBodies: Definitions<RequestBody> = emptyMap(),
  public val headers: Definitions<Header> = emptyMap(),
  //  val securitySchemes: Definitions<SecurityScheme>,
  public val links: Map<String, Link> = emptyMap(),
  public val callbacks: Map<String, Callback> = emptyMap(),
)

@Serializable
public data class PathItem(
  /**
   * Allows for an external definition of this path item. The referenced structure MUST be in the
   * format of a [PathItem]. In case a [PathItem] field appears both in the defined object and the
   * referenced object, the behavior is undefined.
   */
  public val ref: String? = null,
  /** An optional, string summary, intended to apply to all operations in this path. */
  public val summary: String? = null,
  /**
   * An optional, string description, intended to apply to all operations in this path. CommonMark
   * syntax MAY be used for rich text representation.
   */
  public val description: String? = null,
  /** A definition of a GET operation on this path. */
  public val get: Operation? = null,
  /** A definition of a PUT operation on this path. */
  public val put: Operation? = null,
  /** A definition of a POST operation on this path. */
  public val post: Operation? = null,
  /** A definition of a DELETE operation on this path. */
  public val delete: Operation? = null,
  /** A definition of a OPTIONS operation on this path. */
  public val options: Operation? = null,
  /** A definition of a HEAD operation on this path. */
  public val head: Operation? = null,
  /** A definition of a PATCH operation on this path. */
  public val patch: Operation? = null,
  /** A definition of a TRACE operation on this path. */
  public val trace: Operation? = null,
  /** An alternative server array to service all operations in this path. */
  public val servers: List<Server>,
  /**
   * A list of parameters that are applicable for all the operations described under this path.
   * These parameters can be overridden at the operation level, but cannot be removed there. The
   * list MUST NOT include duplicated parameters. A unique parameter is defined by a combination of
   * a name and location. The list can use the Reference Object to link to parameters that are
   * defined at the OpenAPI Object's components/parameters.
   */
  public val parameters: List<Referenced<Parameter>>
) {
  public fun mergeWith(other: PathItem): PathItem =
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

/**
 * Describes a single operation parameter.
 *
 * A unique parameter is defined by a combination of a [name] and [input].
 *
 * Parameter Locations There are four possible parameter locations specified by the in field:
 *
 * path - Used together with Path Templating, where the parameter value is actually part of the
 * operation's URL. This does not include the host or base path of the API. For example, in
 * /items/{itemId}, the path parameter is itemId. query - Parameters that are appended to the URL.
 * For example, in /items?id=###, the query parameter is id. header - Custom headers that are
 * expected as part of the request. Note that RFC7230 states header names are case insensitive.
 * cookie - Used to pass a specific cookie value to the API.
 */
@Serializable
public data class Parameter(
  /**
   * The name of the parameter. Parameter names are case sensitive. If in is "path", the name field
   * MUST correspond to a template expression occurring within the path field in the Paths Object.
   * See [Path Templating](https://swagger.io/specification/#path-templating) for further
   * information. If in is "header" and the name field is "Accept", "Content-Type" or
   * "Authorization", the parameter definition SHALL be ignored. For all other cases, the name
   * corresponds to the parameter name used by the in property.
   */
  public val name: String,
  @SerialName("in")
  /** The input of the parameter.. */
  public val input: ParameterIn,
  /**
   * A brief description of the parameter. This could contain examples of use. CommonMark syntax MAY
   * be used for rich text representation.
   */
  public val description: String? = null,
  /**
   * Determines whether this parameter is mandatory. If the parameter location is "path", this
   * property is REQUIRED and its value MUST be true. Otherwise, the property MAY be included and
   * its default value is false.
   */
  public val required: Boolean = false,
  /**
   * Specifies that a parameter is deprecated and SHOULD be transitioned out of usage. Default value
   * is false.
   */
  public val deprecated: Boolean = false,
  /**
   * Sets the ability to pass empty-valued parameters. This is valid only for query parameters and
   * allows sending a parameter with an empty value. Default value is false. If style is used, and
   * if behavior is n/a (cannot be serialized), the value of allowEmptyValue SHALL be ignored. Use
   * of this property is NOT RECOMMENDED, as it is likely to be removed in a later revision.
   */
  public val allowEmptyValue: Boolean = false,
  /**
   * Determines whether the parameter value SHOULD allow reserved characters, as defined by RFC3986
   * :/?#[]@!$&'()*+,;= to be included without percent-encoding. This property only applies to
   * parameters with an in value of query. The default value is false.
   */
  public val allowReserved: Boolean = false,
  /** The schema defining the type used for the parameter. */
  public val schema: Referenced<Schema>? = null,
  /**
   * Describes how the parameter value will be serialized depending on the type of the parameter
   * value. Default values (based on value of _paramIn): for ParamQuery - StyleForm; for ParamPath -
   * StyleSimple; for ParamHeader - StyleSimple; for ParamCookie - StyleForm.
   */
  public val style: Style? = null,
  public val explode: Boolean? = null,
  /**
   * Example of the parameter's potential value. The example SHOULD match the specified schema and
   * encoding properties if present. The example field is mutually exclusive of the examples field.
   * Furthermore, if referencing a schema that contains an example, the example value SHALL override
   * the example provided by the schema. To represent examples of media types that cannot naturally
   * be represented in JSON or YAML, a string value can contain the example with escaping where
   * necessary.
   */
  public val example: ExampleValue? = null,
  /**
   * Examples of the parameter's potential value. Each example SHOULD contain a value in the correct
   * format as specified in the parameter encoding. The _paramExamples field is mutually exclusive
   * of the _paramExample field. Furthermore, if referencing a schema that contains an example, the
   * examples value SHALL override the example provided by the schema.
   */
  public val examples: Definitions<Example>? = emptyMap()
)

@Serializable
public enum class ParameterIn {
  query,
  header,
  path,
  cookie
}

@Serializable
public data class Operation(
  /**
   * A list of tags for API documentation control. Tags can be used for logical grouping of
   * operations by resources or any other qualifier.
   */
  public val tags: List<String> = emptyList(),
  /** A short summary of what the operation does. */
  public val summary: String? = null,
  /**
   * A verbose explanation of the operation behavior. CommonMark syntax MAY be used for rich text
   * representation.
   */
  public val description: String? = null,
  /** Additional external documentation for this operation. */
  public val externalDocs: ExternalDocs? = null,
  /**
   * Unique string used to identify the operation. The id MUST be unique among all operations
   * described in the API. The operationId value is case-sensitive. Tools and libraries MAY use the
   * operationId to uniquely identify an operation, therefore, it is RECOMMENDED to follow common
   * programming naming conventions.
   */
  public val operationId: String? = null,
  /**
   * A list of parameters that are applicable for this operation. If a parameter is already defined
   * at the Path Item, the new definition will override it but can never remove it. The list MUST
   * NOT include duplicated parameters. A unique parameter is defined by a combination of a name and
   * location. The list can use the Reference Object to link to parameters that are defined at the
   * OpenAPI Object's components/parameters.
   */
  public val parameters: List<Referenced<Parameter>> = emptyList(),
  /**
   * The request body applicable for this operation. The requestBody is only supported in HTTP
   * methods where the HTTP 1.1 specification RFC7231 has explicitly defined semantics for request
   * bodies. In other cases where the HTTP spec is vague, requestBody SHALL be ignored by consumers.
   */
  public val requestBody: Referenced<RequestBody>? = null,
  /** The list of possible responses as they are returned from executing this operation. */
  public val responses: Responses,
  /**
   * A map of possible out-of band callbacks related to the parent operation. The key is a unique
   * identifier for the Callback Object. Each value in the map is a Callback Object that describes a
   * request that may be initiated by the API provider and the expected responses.
   */
  public val callbacks: Definitions<Callback> = emptyMap(),
  /**
   * Declares this operation to be deprecated. Consumers SHOULD refrain from usage of the declared
   * operation. Default value is false.
   */
  public val deprecated: Boolean = false,
  /**
   * A declaration of which security mechanisms can be used for this operation. The list of values
   * includes alternative security requirement objects that can be used. Only one of the security
   * requirement objects need to be satisfied to authorize a request. To make security optional, an
   * empty security requirement ({}) can be included in the array. This definition overrides any
   * declared top-level security. To remove a top-level security declaration, an empty array can be
   * used.
   */
  public val security: List<SecurityRequirement> = emptyList(),
  /**
   * An alternative server array to service this operation. If an alternative server object is
   * specified at the Path Item Object or Root level, it will be overridden by this value.
   */
  public val servers: List<Server> = emptyList()
)

@Serializable
public data class RequestBody(
  /**
   * A brief description of the request body. This could contain examples of use. CommonMark syntax
   * MAY be used for rich text representation.
   */
  public val description: String?,
  /**
   * The content of the request body. The key is a media type or media type range and the value
   * describes it. For requests that match multiple keys, only the most specific key is applicable.
   * e.g. text/plain overrides text
   */
  public val content: Map<String, MediaType>,
  /** Determines if the request body is required in the request. Defaults to false. */
  public val required: Boolean = false
)

/**
 * A container for the expected responses of an operation. The container maps a HTTP response code
 * to the expected response. It is not expected from the documentation to necessarily cover all
 * possible HTTP response codes, since they may not be known in advance. However, it is expected
 * from the documentation to cover a successful operation response and any known errors.
 */
@Serializable(with = ResponsesSerializer::class)
public data class Responses(
  /**
   * The documentation of responses other than the ones declared for specific HTTP response codes.
   * It can be used to cover undeclared responses.
   */
  public val default: Referenced<Response>? = null,
  /**
   * Any HTTP status code can be used as the property name (one property per HTTP status code).
   * Describes the expected response for those HTTP status codes.
   */
  public val responses: Map<StatusCode, Referenced<Response>>
) {
  public operator fun plus(other: Responses): Responses =
    Responses(other.default ?: default, responses + other.responses)
}

@Serializable
public data class Response(
  /**
   * A short description of the response. CommonMark syntax MAY be used for rich text
   * representation.
   */
  public val description: String,
  /**
   * Maps a header name to its definition. RFC7230 states header names are case insensitive. If a
   * response header is defined with the name "Content-Type", it SHALL be ignored.
   */
  public val headers: Definitions<Header> = emptyMap(),
  /**
   * A map containing descriptions of potential response payloads. The key is a media type or media
   * type range and the value describes it. For responses that match multiple keys, only the most
   * specific key is applicable. e.g. text/plain overrides text
   */
  public val content: Map<String, MediaType> = emptyMap(),
  /**
   * A map of operations links that can be followed from the response. The key of the map is a short
   * name for the link, following the naming constraints of the names for Component Objects.
   */
  public val links: Definitions<Link> = emptyMap()
) {
  public operator fun plus(other: Response): Response =
    Response(description, headers + other.headers, content + other.content, links + other.links)
}

/** Each Media Type Object provides schema and examples for the media type identified by its key. */
@Serializable
public data class MediaType(
  public val schema: Referenced<Schema>? = null,
  /** The schema defining the content of the request, response, or parameter. */
  public val example: ExampleValue? = null,
  /**
   * Example of the media type. The example object SHOULD be in the correct format as specified by
   * the media type. The example field is mutually exclusive of the examples field. Furthermore, if
   * referencing a schema which contains an example, the example value SHALL override the example
   * provided by the schema.
   */
  public val examples: Definitions<Example> = emptyMap(),
  /**
   * Examples of the media type. Each example object SHOULD match the media type and specified
   * schema if present. The examples field is mutually exclusive of the example field. Furthermore,
   * if referencing a schema which contains an example, the examples value SHALL override the
   * example provided by the schema.
   */
  public val encoding: Map<String, Encoding> = emptyMap()
/**
 * A map between a property name and its encoding information. The key, being the property name,
 * MUST exist in the schema as a property. The encoding object SHALL only apply to requestBody
 * objects when the media type is multipart or application/x-www-form-urlencoded.
 */
)

/**
 * The Link object represents a possible design-time link for a response. The presence of a link
 * does not guarantee the caller's ability to successfully invoke it, rather it provides a known
 * relationship and traversal mechanism between responses and other operations.
 */
@Serializable
public data class Link(
  /**
   * A relative or absolute URI reference to an OAS operation. This field is mutually exclusive of
   * the '_linkOperationId' field, and MUST point to an 'Operation' Object. Relative
   * '_linkOperationRef' values MAY be used to locate an existing 'Operation' Object in the OpenAPI
   * definition.
   */
  public val operationRef: String? = null,
  /**
   * The name of an /existing/, resolvable OAS operation, as defined with a unique
   * '_operationOperationId'. This field is mutually exclusive of the '_linkOperationRef' field.
   */
  public val operationId: String? = null,
  /**
   * A map representing parameters to pass to an operation as specified with '_linkOperationId' or
   * identified via '_linkOperationRef'. The key is the parameter name to be used, whereas the value
   * can be a constant or an expression to be evaluated and passed to the linked operation. The
   * parameter name can be qualified using the parameter location @[{in}.]{name}@ for operations
   * that use the same parameter name in different locations (e.g. path.id).
   */
  public val parameters: Map<String, ExpressionOrValue>,
  /**
   * A literal value or @{expression}@ to use as a request body when calling the target operation.
   */
  public val requestBody: ExpressionOrValue,
  /** A description of the link. */
  public val description: String? = null,
  /** A server object to be used by the target operation. */
  public val server: Server?
)

/** A single encoding definition applied to a single schema property. */
@Serializable
public data class Encoding(
  /**
   * The Content-Type for encoding a specific property. Default value depends on the property type:
   * - for string with format being binary – application/octet-stream;
   * - for other primitive types – text/plain
   * - for object - application/json
   * - for array – the default is defined based on the inner type. The value can be a specific media
   * type (e.g. application/json), a wildcard media type (e.g. image&#47;&#42;), or a
   * comma-separated list of the two types.
   */
  public val contentType: String, // Could be arrow.endpoint.model.MediaType
  /**
   * A map allowing additional information to be provided as headers, for example
   * Content-Disposition. Content-Type is described separately and SHALL be ignored in this section.
   * This property SHALL be ignored if the request body media type is not a multipart
   */
  public val headers: Definitions<Header>,
  /**
   * Describes how a specific property value will be serialized depending on its type. See [Style]
   * for details on the style property. The behavior follows the same values as query parameters,
   * including default values. This property SHALL be ignored if the request body media type is not
   * application/x-www-form-urlencoded.
   */
  public val style: String? = null,
  /**
   * When this is true, property values of type array or object generate separate parameters for
   * each value of the array, or key-value-pair of the map. For other types of properties this
   * property has no effect. When style is form, the default value is true. For all other styles,
   * the default value is false. This property SHALL be ignored if the request body media type is
   * not application/x-www-form-urlencoded.
   */
  public val explode: Boolean, // = style?.let { it == Style.form.name } ?: false,
  /**
   * Determines whether the parameter value SHOULD allow reserved characters, as defined by RFC3986
   * :/?#[]@!$&'()*+,;= to be included without percent-encoding. The default value is false. This
   * property SHALL be ignored if the request body media type is not
   * application/x-www-form-urlencoded.
   */
  public val allowReserved: Boolean
)

/**
 * Header fields have the same meaning as for 'Param'. Style is always treated as [Style.simple], as
 * it is the only value allowed for headers.
 */
@Serializable
public data class Header(
  /** A short description of the header. */
  public val description: String? = null,
  public val required: Boolean? = null,
  public val deprecated: Boolean? = null,
  public val allowEmptyValue: Boolean? = null,
  public val explode: Boolean? = null,
  public val example: ExampleValue? = null,
  public val examples: Definitions<Example>? = null,
  public val schema: Referenced<Schema>? = null
)

/**
 * A map of possible out-of band callbacks related to the parent operation. Each value in the map is
 * a [PathItem] Object that describes a set of requests that may be initiated by the API provider
 * and the expected responses. The key value used to identify the path item object is an expression,
 * evaluated at runtime, that identifies a URL to use for the callback operation.
 */
@Serializable @JvmInline public value class Callback(public val value: Map<String, PathItem>)

@Serializable
public data class Discriminator(val propertyName: String, val mapping: Map<String, String>? = null)

@Serializable
@Suppress("EnumEntryName")
public enum class OpenApiType {
  boolean,
  `object`,
  array,
  number,
  string,
  integer
}

@Suppress("MayBeConstant")
public object Format {
  public const val int32: String = "int32"
  public const val int64: String = "int64"
  public const val float: String = "float"
  public const val double: String = "double"
  public const val byte: String = "byte"
  public const val binary: String = "binary"
  public const val date: String = "date"
  public const val datetime: String = "datetime"
  public const val password: String = "password"
}

@Serializable
@Suppress("EnumEntryName")
public enum class Style {
  simple,
  form,
  matrix,
  label,
  spaceDelimited,
  pipeDelimited,
  deepObject
}

@Serializable
public data class Example(
  /** Short description for the example. */
  public val summary: String? = null,
  /**
   * Long description for the example. CommonMark syntax MAY be used for rich text representation.
   */
  public val description: String? = null,
  /**
   * Embedded literal example. The value field and externalValue field are mutually exclusive. To
   * represent examples of media types that cannot naturally represented in JSON or YAML, use a
   * string value to contain the example, escaping where necessary.
   */
  public val value: ExampleValue? = null,
  /**
   * A URL that points to the literal example. This provides the capability to reference examples
   * that cannot easily be included in JSON or YAML documents. The value field and externalValue
   * field are mutually exclusive.
   */
  public val externalValue: String? = null
)

/** Allows referencing an external resource for extended documentation. */
@Serializable
public data class ExternalDocs(
  /**
   * A short description of the target documentation. CommonMark syntax MAY be used for rich text
   * representation.
   */
  public val description: String? = null,
  /** The URL for the target documentation. Value MUST be in the format of a URL. */
  public val url: String
)

/** Contact information for the exposed API. */
@Serializable
public data class Contact(
  /** The identifying name of the contact person/organization. */
  public val name: String? = null,
  /** The URL pointing to the contact information. MUST be in the format of a URL. */
  public val url: String? = null,
  /**
   * The email address of the contact person/organization. MUST be in the format of an email
   * address.
   */
  public val email: String? = null
)

/** License information for the exposed API. */
@Serializable
public data class License(
  /** The license name used for the API. */
  public val name: String,
  /** A URL to the license used for the API. MUST be in the format of a URL. */
  public val url: String?
)

internal const val RefKey = "\$ref"

@Serializable
public data class Reference(@SerialName(RefKey) val ref: String) {
  public companion object {
    public operator fun invoke(prefix: String, ref: String): Reference = Reference("$prefix$ref")
  }
}

@Serializable
public data class Xml(
  /**
   * Replaces the name of the element/attribute used for the described schema property. When defined
   * within the @'OpenApiItems'@ (items), it will affect the name of the individual XML elements
   * within the list. When defined alongside type being array (outside the items), it will affect
   * the wrapping element and only if wrapped is true. If wrapped is false, it will be ignored.
   */
  val name: String,
  /** The URL of the namespace definition. Value SHOULD be in the form of a URL. */
  val namespace: String? = null,
  /** The prefix to be used for the name. */
  val prefix: String?,
  /**
   * Declares whether the property definition translates to an attribute instead of an element.
   * Default value is @False@.
   */
  val attribute: Boolean? = null,
  /**
   * MAY be used only for an array definition. Signifies whether the array is wrapped (for example,
   * @\<books\>\<book/\>\<book/\>\</books\>@) or unwrapped (@\<book/\>\<book/\>@). Default value is
   * @False@. The definition takes effect only when defined alongside type being array (outside the
   * items).
   */
  val wrapped: Boolean? = null
)

/**
 * The Schema Object allows the definition of input and output data types. These types can be
 * objects, but also primitives and arrays. This object is an extended subset of the
 * [JSON Schema Specification Wright Draft 00](https://json-schema.org/). For more information about
 * the properties, see [JSON Schema Core](https://tools.ietf.org/html/draft-wright-json-schema-00)
 * and [JSON Schema Validation](https://tools.ietf.org/html/draft-wright-json-schema-validation-00).
 * Unless stated otherwise, the property definitions follow the JSON Schema.
 */
@Serializable
public data class Schema(
  val title: String? = null,
  val description: String? = null,
  val required: List<String> = emptyList(),
  val nullable: Boolean? = null,
  val allOf: List<Referenced<Schema>>? = null,
  val oneOf: List<Referenced<Schema>>? = null,
  val not: Referenced<Schema>? = null,
  val anyOf: List<Referenced<Schema>>? = null,
  val properties: Definitions<Schema> = emptyMap(),
  val additionalProperties: AdditionalProperties? = null,
  val discriminator: Discriminator? = null,
  val readOnly: Boolean? = null,
  val writeOnly: Boolean? = null,
  val xml: Xml? = null,
  val externalDocs: ExternalDocs? = null,
  val example: ExampleValue? = null,
  val deprecated: Boolean? = null,
  val maxProperties: Int? = null,
  val minProperties: Int? = null,
  /**
   * Declares the value of the parameter that the server will use if none is provided, for example a
   * @"count"@ to control the number of results per page might default to @100@ if not supplied by
   * the client in the request. (Note: "default" has no meaning for required parameters.) Unlike
   * JSON Schema this value MUST conform to the defined type for this parameter.
   */
  val default: ExampleValue? = null,
  val type: OpenApiType? = null,
  val format: String? = null,
  val items: Referenced<Schema>? = null,
  val maximum: BigDecimal? = null,
  val exclusiveMaximum: Boolean? = null,
  val minimum: BigDecimal? = null,
  val exclusiveMinimum: Boolean? = null,
  val maxLength: Int? = null,
  val minLength: Int? = null,
  val pattern: String? = null,
  val maxItems: Int? = null,
  val minItems: Int? = null,
  val uniqueItems: Boolean? = null,
  val enum: List<String> = emptyList(),
  val multipleOf: BigDecimal? = null
)

public data class ExternalDocumentation(
  public val url: String,
  public val description: String? = null
)

@Serializable(with = ExampleValueSerializer::class)
public sealed class ExampleValue {

  public data class Single(public val value: String) : ExampleValue()
  public data class Multiple(public val values: List<String>) : ExampleValue()

  public companion object {
    public operator fun invoke(v: String): ExampleValue = Single(v)
    @Suppress("UNCHECKED_CAST")
    public operator fun invoke(codec: Codec<*, *, *>, e: Any?): ExampleValue? =
      invoke(codec.schema(), (codec as Codec<*, Any?, *>).encode(e))

    public operator fun invoke(schema: arrow.endpoint.Schema<*>, raw: Any?): ExampleValue? =
      when (raw) {
        is Iterable<*> ->
          when (schema) {
            is arrow.endpoint.Schema.List -> Multiple(raw.map(Any?::toString))
            else -> raw.firstOrNull()?.let { Single(it.toString()) }
          }
        is Option<*> -> raw.fold({ null }) { Single(it.toString()) }
        null -> null
        else -> Single(raw.toString())
      }
  }
}

/**
 * Defines Union: [A] | [Reference] type of OpenAPI. Defined here instead of using Either since it's
 * more convenient to define a KotlinX serializer here.
 */
@Serializable(with = ReferencedSerializer::class)
public sealed class Referenced<out A> {
  public data class Ref(public val value: Reference) : Referenced<Nothing>()
  public data class Other<A>(val value: A) : Referenced<A>()

  public fun <B> map(f: (A) -> B): Referenced<B> =
    when (this) {
      is Other -> Other(f(value))
      is Ref -> this
    }
}

public sealed interface ExpressionOrValue {
  @JvmInline public value class Expression(public val value: String) : ExpressionOrValue
  @JvmInline public value class Value(public val value: Any?) : ExpressionOrValue
}

public sealed interface AdditionalProperties {
  @JvmInline public value class Allowed(public val value: Boolean) : AdditionalProperties
  @JvmInline
  public value class PSchema(public val value: Referenced<Schema>) : AdditionalProperties
}
