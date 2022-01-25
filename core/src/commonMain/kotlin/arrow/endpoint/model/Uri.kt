@file:Suppress("MemberVisibilityCanBePrivate")

package arrow.endpoint.model

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.left
import arrow.core.right
import arrow.endpoint.model.Rfc3986.decode
import arrow.endpoint.model.Rfc3986.encode
import kotlin.jvm.JvmInline

/**
 * A [https://en.wikipedia.org/wiki/Uniform_Resource_Identifier URI]. Can represent both relative and absolute
 * URIs, hence in terms of [https://tools.ietf.org/html/rfc3986], this is a URI reference.
 *
 * All components (scheme, host, query, ...) are stored decoded, and become encoded upon serialization
 * (using [toString]).
 *
 * Instances can be created using the factory methods on the [Uri] public companion object.
 *
 * The `invoke`/`parse` methods create absolute URIs and require a host.
 *
 * @param querySegments Either key-value pairs, single values, or plain
 * query segments. Key value pairs will be serialized as `k=v`, and blocks
 * of key-value pairs/single values will be combined using `&`. Note that no
 * `&` or other separators are added around plain query segments - if
 * required, they need to be added manually as part of the plain query
 * segment. Custom encoding logic can be provided when creating a segment.
 */
public data class Uri(
  val scheme: String,
  val authority: Authority?,
  val pathSegments: PathSegments,
  val querySegments: List<QuerySegment>,
  val fragmentSegment: FragmentSegment?
) {

  public companion object {
    private val schemePattern =
      Regex("^([a-zA-Z][a-zA-Z0-9+\\-.]*):")

    @Suppress("RegExpRedundantEscape")
    private val schemeSpecificPartPattern =
      Regex("^?(//(?<authority>((?<userinfo>[^/?#]*)@)?(?<host>(\\[[^\\]]*\\]|[^/?#:]*))(:(?<port>[^/?#]*))?))?(?<path>[^?#]*)(\\?(?<query>[^#]*))?(#(?<fragment>.*))?")

    private val uriPartsRegex =
      Regex("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?")

    public operator fun invoke(url: String): Uri? =
      parse(url).orNull()

    public fun parse(url: String): Either<UriError, Uri> =
      either.eager {
        val trimmedUrl = url.trimStart()
        val scheme = schemePattern.find(trimmedUrl)?.value?.substringBefore(':')?.lowercase() ?: ""

        val schemeSpecificPart = when (scheme) {
          "http", "https" -> trimmedUrl.substring(scheme.length + 1).lowercase()
          else -> UriError.UnexpectedScheme("Unexpected scheme: $scheme").left().bind()
        }

        val match: MatchResult = schemeSpecificPartPattern.matchEntire(schemeSpecificPart)
          ?: UriError.CantParse("Can't parse $trimmedUrl").left().bind()

        Uri(
          scheme = scheme.decode().bind(),
          authority = Authority(
            userInfo = getUserInfoOrNull(match)?.bind(),
            hostSegment = getHost(match).bind(),
            port = getPort(match, scheme)?.bind(),
          ),
          pathSegments = getPathSegmentsOrEmpty(match).bind(),
          querySegments = getQuerySegmentsOrEmpty(match).bind(),
          fragmentSegment = getFragmentSegmentOrNull(match).bind()
        )
      }

    private fun getUserInfoOrNull(match: MatchResult): Either<UriError, UserInfo>? =
      (match.groups as? MatchNamedGroupCollection)?.get("userinfo")?.value?.let { value ->
        value.split(":").let { userInfoParts ->
          when {
            userInfoParts.isEmpty() -> return null
            else -> UserInfo(
              userInfoParts.first().decode().fold({ return it.left() }, { it }),
              userInfoParts.drop(1).lastOrNull()?.decode()?.fold({ return it.left() }, { it })
            )
          }
        }.right()
      }

    private fun getHost(match: MatchResult): Either<UriError, HostSegment> =
      (match.groups as? MatchNamedGroupCollection)?.get("host")?.value?.let { value ->
        value.removeSurrounding(prefix = "[", suffix = "]").let { host: String ->
          if (host.isNotEmpty() && host != " " && host != "\n" && host != "%20") HostSegment(
            v = host.decode().fold({ return it.left() }, { it })
          ).right()
          else UriError.InvalidHost.left()
        }
      } ?: UriError.InvalidHost.left()

    private fun getPort(match: MatchResult, scheme: String): Either<UriError, Int>? =
      (match.groups as? MatchNamedGroupCollection)?.get("port")?.value?.let { value ->
        val port: Int? = value.let {
          when {
            it.isEmpty() -> null
            else -> {
              try {
                it.toInt()
              } catch (ex: NumberFormatException) {
                return UriError.InvalidPort.left()
              }
            }
          }
        }
        when {
          port == null || port.isDefaultPort(scheme) -> null // we can omit it
          port in 1..65535 -> port.right()
          else -> UriError.InvalidPort.left()
        }
      }

    private fun Int.isDefaultPort(scheme: String) = when (scheme) {
      "https" -> 443 == this
      else -> 80 == this
    }

    private fun getPathSegmentsOrEmpty(match: MatchResult): Either<UriError, PathSegments> =
      PathSegments.absoluteOrEmptyS(
        (match.groups as? MatchNamedGroupCollection)?.get("path")?.value?.let { pathPart ->
          when {
            pathPart.isEmpty() -> emptyList()
            else -> pathPart.removePrefix("/").split("/")
              .map { segment -> segment.decode().fold({ return it.left() }, { it }) }
          }
        } ?: emptyList()
      ).right()

    private fun getQuerySegmentsOrEmpty(
      match: MatchResult
    ): Either<UriError, List<QuerySegment>> =
      (match.groups as? MatchNamedGroupCollection)?.get("query")?.value?.let { querySegments ->
        when (querySegments.contains("&") || querySegments.contains("=")) {
          true -> {
            querySegments.split("&").map { querySegment ->
              querySegment.split("=").map { it.decode(plusAsSpace = true).fold({ e -> return e.left() }, { a -> a }) }
            }.map { listQueryParams: List<String> ->
              when (listQueryParams.size) {
                1 -> QuerySegment.Value(listQueryParams.first())
                else -> QuerySegment.KeyValue(
                  listQueryParams.first(),
                  buildString { append(listQueryParams.drop(1).joinToString("=")) }
                )
              }
            }
          }
          false -> listOf(QuerySegment.Plain(querySegments.decode().fold({ return it.left() }, { it })))
        }.right()
      } ?: emptyList<QuerySegment>().right()

    private fun getFragmentSegmentOrNull(match: MatchResult): Either<UriError, FragmentSegment?> =
      (match.groups as? MatchNamedGroupCollection)?.get("fragment")?.value?.let { fragment ->
        when (fragment.isNotEmpty()) {
          true -> FragmentSegment(v = fragment.decode().fold({ return it.left() }, { it }))
          false -> null
        }.right()
      } ?: null.right()
  }

  /** Replace the scheme. Does not validate the new scheme value. */
  public fun scheme(s: String): Uri = this.copy(scheme = s)

  /** Replace the user info with a username only. Adds an empty host if one is absent. */
  public fun userInfo(username: String): Uri = userInfo(UserInfo(username, null))

  /** Replace the user info with username/password combination. Adds an empty host if one is absent. */
  public fun userInfo(username: String, password: String): Uri = userInfo(UserInfo(username, password))

  /** Replace the user info with username/password combination. Adds an empty host if one is absent, and user info
   * is defined.
   */
  public fun userInfo(ui: UserInfo?): Uri =
    this.copy(authority = authority?.copy(userInfo = ui) ?: Authority(userInfo = ui))

  public fun userInfo(): UserInfo? = authority?.userInfo

  /** Replace the host. Does not validate the new host value if it's nonempty. */
  public fun host(h: String): Uri =
    this.copy(authority = authority?.copy(hostSegment = HostSegment(h)))

  public fun host(): String? = authority?.hostSegment?.v

  /** Replace the port. Adds an empty host if one is absent, and port is defined. */
  public fun port(p: Int?): Uri =
    this.copy(authority = authority?.copy(port = p) ?: Authority(port = p))

  public fun port(): Int? = authority?.port

  /** Replace the authority. */
  public fun authority(a: Authority): Uri =
    this.copy(authority = a)

  public fun addPath(p: String, vararg ps: String): Uri =
    addPathSegments(listOf(PathSegment(p)) + ps.map { PathSegment(it) })

  public fun addPathSegments(ss: List<PathSegment>): Uri = copy(pathSegments = pathSegments.addSegments(ss))

  public fun withPath(p: String, vararg ps: String): Uri =
    withPathSegments(listOf(PathSegment(p)) + ps.map { PathSegment(it) })

  public fun withPathSegments(ss: List<PathSegment>): Uri = copy(pathSegments = pathSegments.withSegments(ss))

  /** Replace the whole path with the given one. Leading `/` will be removed, if present, and the path will be
   * split into segments on `/`.
   */
  public fun withWholePath(p: String): Uri {
    // removing the leading slash, as it is added during serialization anyway
    val pWithoutLeadingSlash = if (p.startsWith("/")) p.substring(1) else p
    val ps = pWithoutLeadingSlash.split("/", limit = -1)
    return if (ps.isEmpty()) this else withPathSegments(ps.map { PathSegment(it) })
  }

  public fun path(): List<String> = pathSegments.segments.map { it.v }

  public fun addParam(k: String, v: String?): Uri = v?.let { addParams(listOf(Pair(k, v))) } ?: this

  public fun addParams(ps: Map<String, String>): Uri = addParams(ps.toList())

  public fun addParams(mqp: QueryParams): Uri =
    this.copy(querySegments = querySegments + QuerySegment.fromQueryParams(mqp))

  public fun addParams(ps: List<Pair<String, String>>): Uri =
    this.copy(querySegments = querySegments + ps.map { (k, v) -> QuerySegment.KeyValue(k, v) })

  /** Replace query with the given single optional parameter. */
  public fun withParam(k: String, v: String?): Uri = v?.let { withParams(listOf(Pair(k, v))) } ?: this

  /** Replace query with the given parameters. */
  public fun withParams(ps: Map<String, String>): Uri = withParams(ps.toList())

  /** Replace query with the given parameters. */
  public fun withParams(mqp: QueryParams): Uri =
    this.copy(querySegments = QuerySegment.fromQueryParams(mqp).toList())

  /** Replace query with the given parameters. */
  public fun withParams(ps: List<Pair<String, String>>): Uri =
    this.copy(querySegments = ps.map { (k, v) -> QuerySegment.KeyValue(k, v) })

  public fun paramsMap(): Map<String, String> = paramsSeq().toMap()

  public fun params(): QueryParams {
    val m = linkedMapOf<String, List<String>>() // keeping parameter order
    querySegments.forEach {
      when (it) {
        is QuerySegment.KeyValue -> m[it.k] = m.getOrElse(it.k) { emptyList() } + listOf(it.v)
        is QuerySegment.Value -> m[it.v] = m.getOrElse(it.v) { emptyList() }
        is QuerySegment.Plain -> m[it.v] = m.getOrElse(it.v) { emptyList() }
      }
    }
    return QueryParams(m.toList())
  }

  public fun paramsSeq(): List<Pair<String, String>> = params().toList()

  public fun addQuerySegment(qf: QuerySegment): Uri = this.copy(querySegments = querySegments + listOf(qf))

  /** Replace the fragment. */
  public fun fragment(f: String?): Uri =
    fragmentSegment(f?.let { FragmentSegment(it) })

  /** Replace the fragment. */
  public fun fragmentSegment(s: FragmentSegment?): Uri = this.copy(fragmentSegment = s)

  public fun fragment(): String? = fragmentSegment?.v


  public fun hostSegmentEncoding(encoding: Encoding): Uri =
    copy(authority = authority?.copy(hostSegment = authority.hostSegment.encoding(encoding)))

  public fun pathSegmentsEncoding(encoding: Encoding): Uri =
    copy(
      pathSegments = when (pathSegments) {
        is PathSegments.EmptyPath -> PathSegments.EmptyPath
        is PathSegments.AbsolutePath -> PathSegments.AbsolutePath(pathSegments.segments.map { it.encoding(encoding) })
        is PathSegments.RelativePath -> PathSegments.RelativePath(pathSegments.segments.map { it.encoding(encoding) })
      }
    )

  /** Replace encoding for query segments: applies to key-value, only-value and plain ones. */
  public fun querySegmentsEncoding(encoding: Encoding): Uri =
    copy(
      querySegments = querySegments.map {
        when (it) {
          is QuerySegment.KeyValue -> QuerySegment.KeyValue(it.k, it.v, encoding, encoding)
          is QuerySegment.Value -> QuerySegment.Value(it.v, encoding)
          is QuerySegment.Plain -> QuerySegment.Plain(it.v, encoding)
        }
      }
    )

  /** Replace encoding for the value part of key-value query segments and for only-value ones. */
  public fun queryValueSegmentsEncoding(valueEncoding: Encoding): Uri =
    copy(
      querySegments = querySegments.map {
        when (it) {
          is QuerySegment.KeyValue -> QuerySegment.KeyValue(it.k, it.v, it.keyEncoding, valueEncoding)
          is QuerySegment.Value -> QuerySegment.Value(it.v, valueEncoding)
          is QuerySegment.Plain -> QuerySegment.Plain(it.v, valueEncoding)
        }
      }
    )

  public fun fragmentSegmentEncoding(encoding: Encoding): Uri =
    copy(fragmentSegment = fragmentSegment?.encoding(encoding))

  override fun toString(): String {
    tailrec fun StringBuilder.encodeQuerySegments(qss: List<QuerySegment>, previousWasPlain: Boolean): String =
      when (val headQuerySegment = qss.firstOrNull()) {
        null -> toString()
        is QuerySegment.Plain -> {
          append(headQuerySegment.encoding(headQuerySegment.v))
          encodeQuerySegments(qss.drop(1), previousWasPlain = true)
        }
        is QuerySegment.Value -> {
          if (!previousWasPlain) append("&")
          append(headQuerySegment.encoding(headQuerySegment.v))
          encodeQuerySegments(qss.drop(1), previousWasPlain = false)
        }
        is QuerySegment.KeyValue -> {
          if (!previousWasPlain) append("&")
          append(headQuerySegment.keyEncoding(headQuerySegment.k)).append("=")
            .append(headQuerySegment.valueEncoding(headQuerySegment.v))
          encodeQuerySegments(qss.drop(1), previousWasPlain = false)
        }
      }

    val schemeS = "${scheme.encode(Rfc3986.Scheme)}:"
    val authorityS = authority?.toString() ?: ""
    val pathPrefixS = when {
      pathSegments is PathSegments.AbsolutePath -> "/"
      authority == null -> ""
      pathSegments is PathSegments.EmptyPath -> ""
      pathSegments is PathSegments.RelativePath -> ""
      else -> ""
    }
    val pathS = pathSegments.segments.joinToString("/") { it.encoded() }
    val queryPrefixS = if (querySegments.isEmpty()) "" else "?"

    val queryS = buildString { encodeQuerySegments(querySegments, previousWasPlain = true) }

    // https://stackoverflow.com/questions/2053132/is-a-colon-safe-for-friendly-url-use/2053640#2053640
    val fragS = fragmentSegment?.let { "#" + it.encoded() } ?: ""

    return "$schemeS$authorityS$pathPrefixS$pathS$queryPrefixS$queryS$fragS"
  }
}

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

public data class UserInfo(val username: String, val password: String?) {
  override fun toString(): String =
    "${username.encode(Rfc3986.UserInfo)}${password?.let { ":${it.encode(Rfc3986.UserInfo)}" } ?: ""}"
}

public typealias Encoding = (String) -> String

public sealed class Segment(
  public open val v: String,
  public open val encoding: Encoding
) {
  public fun encoded(): String = encoding(v)
  public abstract fun encoding(e: Encoding): Segment
}

public data class HostSegment(
  override val v: String,
  override val encoding: Encoding = Standard
) : Segment(v, encoding) {

  public companion object {
    private val IpV6Pattern: Regex = "[0-9a-fA-F:]+".toRegex()
    public val Standard: Encoding = { s ->
      when {
        s.matches(IpV6Pattern) && s.count { it == ':' } >= 2 -> "[$s]"
        else -> UriCompatibility.encodeDNSHost(s)
      }
    }
  }

  override fun encoding(e: Encoding): HostSegment = copy(encoding = e)
}

public data class PathSegment(
  override val v: String,
  override val encoding: Encoding = Standard
) : Segment(v, encoding) {

  public companion object {
    public val Standard: Encoding = {
      it.encode(Rfc3986.PathSegment)
    }
  }

  override fun encoding(e: Encoding): PathSegment = copy(encoding = e)
}

public sealed interface PathSegments {

  public val segments: List<PathSegment>

  public companion object {
    public fun absoluteOrEmptyS(segments: List<String>): PathSegments =
      absoluteOrEmpty(segments.map { PathSegment(it) })

    public fun absoluteOrEmpty(segments: List<PathSegment>): PathSegments =
      if (segments.isEmpty()) EmptyPath else AbsolutePath(segments)
  }

  public fun add(p: String, vararg ps: String): PathSegments = add(listOf(p) + ps)
  public fun add(ps: List<String>): PathSegments = addSegments(ps.map { PathSegment(it) })
  public fun addSegment(s: PathSegment): PathSegments = addSegments(listOf(s))
  public fun addSegments(s1: PathSegment, s2: PathSegment, ss: List<PathSegment>): PathSegments =
    addSegments(listOf(s1, s2) + ss)

  public fun addSegments(ss: List<PathSegment>): PathSegments {
    val base = if (segments.lastOrNull()?.v?.isEmpty() == true) emptyList() else segments
    return withSegments(base + ss)
  }

  public fun withS(p: String, ps: Sequence<String>): PathSegments = withS(listOf(p) + ps)
  public fun withS(ps: List<String>): PathSegments = withSegments(ps.map { PathSegment(it) })

  public fun withSegment(s: PathSegment): PathSegments = withSegments(listOf(s))
  public fun withSegments(s1: PathSegment, s2: PathSegment, ss: List<PathSegment>): PathSegments =
    withSegments(listOf(s1, s2) + ss)

  public fun withSegments(ss: List<PathSegment>): PathSegments

  public object EmptyPath : PathSegments {
    override val segments: List<PathSegment> = emptyList()
    override fun withSegments(ss: List<PathSegment>): PathSegments = AbsolutePath(ss)
    override fun toString(): String = ""
  }

  public data class AbsolutePath(override val segments: List<PathSegment>) : PathSegments {
    override fun withSegments(ss: List<PathSegment>): AbsolutePath = copy(segments = ss)
    override fun toString(): String = segments.joinToString(separator = "/", prefix = "/") { it.encoded() }
  }

  public data class RelativePath(override val segments: List<PathSegment>) : PathSegments {
    override fun withSegments(ss: List<PathSegment>): RelativePath = copy(segments = ss)
    override fun toString(): String = segments.joinToString(separator = "/") { it.encoded() }
  }
}

public sealed interface QuerySegment {

  public companion object {
    /** Encodes all reserved characters [jvm target] using [java.net.URLEncoder.encode]. */
    public val All: Encoding
      get() = {
        UriCompatibility.encodeQuery(it, "UTF-8")
      }

    /** Encodes only the `&` and `=` reserved characters, which are usually used to separate query parameter names and
     * values.
     */
    public val Standard: Encoding = {
      it.encode(allowedCharacters = Rfc3986.Query - setOf('&', '='), spaceAsPlus = true, encodePlus = true)
    }

    /** Encodes only the `&` reserved character, which is usually used to separate query parameter names and values.
     * The '=' sign is allowed in values.
     */
    public val StandardValue: Encoding = {
      it.encode(Rfc3986.Query - setOf('&'), spaceAsPlus = true, encodePlus = true)
    }

    /** Doesn't encode any of the reserved characters, leaving intact all
     * characters allowed in the query string as defined by RFC3986.
     */
    public val Relaxed: Encoding = {
      it.encode(Rfc3986.Query, spaceAsPlus = true)
    }

    /** Doesn't encode any of the reserved characters, leaving intact all
     * characters allowed in the query string as defined by RFC3986 as well
     * as the characters `[` and `]`. These brackets aren't legal in the
     * query part of the URI, but some servers use them unencoded. See
     * https://stackoverflow.com/questions/11490326/is-array-syntax-using-square-brackets-in-url-query-strings-valid
     * for discussion.
     */
    public val RelaxedWithBrackets: Encoding = {
      it.encode(Rfc3986.SegmentWithBrackets, spaceAsPlus = true)
    }

    public fun fromQueryParams(mqp: QueryParams): Iterable<QuerySegment> =
      mqp.toMultiList().flatMap { (k: String, vs: List<String>) ->
        when {
          vs.isEmpty() -> listOf(Value(k))
          else -> vs.map { v -> KeyValue(k, v) }
        }
      }
  }

  /**
   * @param keyEncoding See [Plain.encoding]
   * @param valueEncoding See [Plain.encoding]
   */
  public data class KeyValue(
    val k: String,
    val v: String,
    val keyEncoding: Encoding = Standard,
    val valueEncoding: Encoding = Standard
  ) : QuerySegment {
    override fun toString(): String = "KeyValue($k, $v)"
  }

  /** A query fragment which contains only the value, without a key. */
  public data class Value(
    val v: String,
    val encoding: Encoding = StandardValue
  ) : QuerySegment {
    override fun toString(): String = "Value($v)"
  }

  /**
   * A query fragment which will be inserted into the query, without and
   * preceding or following separators. Allows constructing query strings
   * which are not (only) &-separated key-value pairs.
   *
   * @param encoding How to encode the value, and which characters should be escaped. The RFC3986 standard
   * defines that the query can include these special characters, without escaping:
   *
   * ```
   * /?:@-._~!$&()*+,;=
   * ```
   *
   * @url https://stackoverflow.com/questions/2322764/what-characters-must-be-escaped-in-an-http-query-string
   * @url https://stackoverflow.com/questions/2366260/whats-valid-and-whats-not-in-a-uri-query
   */
  public data class Plain(
    val v: String,
    val encoding: Encoding = StandardValue
  ) : QuerySegment {
    override fun toString(): String = "Plain($v)"
  }
}

public data class FragmentSegment(
  override val v: String,
  override val encoding: Encoding = Standard
) : Segment(v, encoding) {

  public companion object {
    public val Standard: Encoding = {
      it.encode(Rfc3986.Fragment)
    }

    public val RelaxedWithBrackets: Encoding = {
      it.encode(Rfc3986.SegmentWithBrackets, spaceAsPlus = true)
    }
  }

  override fun encoding(e: Encoding): FragmentSegment = copy(encoding = e)
}
