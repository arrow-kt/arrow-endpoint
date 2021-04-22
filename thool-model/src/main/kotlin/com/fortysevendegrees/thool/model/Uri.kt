package com.fortysevendegrees.thool.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fortysevendegrees.thool.model.Rfc3986.decode
import com.fortysevendegrees.thool.model.Rfc3986.encode
import java.net.URI

/**
 * A [[https://en.wikipedia.org/wiki/Uniform_Resource_Identifier URI]]. Can represent both relative and absolute
 * URIs, hence in terms of [[https://tools.ietf.org/html/rfc3986]], this is a URI reference.
 *
 * All components (scheme, host, query, ...) are stored decoded, and become encoded upon serialization
 * (using [[toString]]).
 *
 * Instances can be created using the uri interpolator: `uri"..."` (see [[UriInterpolator]]), or the factory methods
 * on the [[Uri]] public companion object.
 *
 * The `apply`/`safeApply`/`unsafeApply` methods create absolute URIs and require a host.
 * The `relative` methods creates a relative URI, given path/query/fragment components.
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

    private val schemeSpecificPartPattern =
      Regex("^?(//(?<authority>((?<userinfo>[^/?#]*)@)?(?<host>(\\[[^\\]]*\\]|[^/?#:]*))(:(?<port>[^/?#]*))?))?(?<path>[^?#]*)(\\?(?<query>[^#]*))?(#(?<fragment>.*))?")

    operator fun invoke(javaUri: URI): Uri? =
      parse(javaUri.toString()).orNull()

    operator fun invoke(url: String): Uri? =
      parse(url).orNull()

    fun parse(url: String): Either<UriError, Uri> {
      val trimmedUrl = url.trimStart()
      val scheme = schemePattern.find(trimmedUrl)?.value?.substringBefore(':')?.toLowerCase() ?: ""

      val schemeSpecificPart = when (scheme) {
        "http", "https" -> trimmedUrl.substring(scheme.length + 1).toLowerCase()
        else -> return UriError.UnexpectedScheme("Unexpected scheme: $scheme").left()
      }

      val match: MatchResult = schemeSpecificPartPattern.matchEntire(schemeSpecificPart)
        ?: return UriError.CantParse("Can't parse $trimmedUrl").left()

      return Uri(
        scheme = scheme.decode().fold({ return it.left() }, { it }),
        authority = Authority(
          userInfo = getUserInfoOrNull(match, schemeSpecificPart)?.fold({ return it.left() }, { it }),
          hostSegment = getHost(match, schemeSpecificPart).fold({ return it.left() }, { it }),
          port = getPort(match, schemeSpecificPart, scheme)?.fold({ return it.left() }, { it }),
        ),
        pathSegments = getPathSegmentsOrEmpty(match, schemeSpecificPart).fold({ return it.left() }, { it }),
        querySegments = getQuerySegmentsOrEmpty(match, schemeSpecificPart).fold({ return it.left() }, { it }),
        fragmentSegment = getFragmentSegmentOrNull(match, schemeSpecificPart).fold({ return it.left() }, { it })
      ).right()
    }

    private fun getUserInfoOrNull(match: MatchResult, schemeSpecificPart: String): Either<UriError, UserInfo>? =
      match.groups["userinfo"]?.range?.let { range ->
        schemeSpecificPart.substring(range).split(":").let { userInfoParts ->
          when {
            userInfoParts.isEmpty() -> return null
            else -> UserInfo(
              userInfoParts.first().decode().fold({ return it.left() }, { it }),
              userInfoParts.drop(1).lastOrNull()?.decode()?.fold({ return it.left() }, { it })
            )
          }
        }.right()
      }

    private fun getHost(match: MatchResult, schemeSpecificPart: String): Either<UriError, HostSegment> =
      match.groups["host"]?.range?.let { range ->
        schemeSpecificPart.substring(range).removeSurrounding(prefix = "[", suffix = "]").let { host: String ->
          if (host.isNotEmpty() && host != " " && host != "\n" && host != "%20") HostSegment(
            v = host.decode().fold({ return it.left() }, { it })
          ).right()
          else UriError.InvalidHost.left()
        }
      } ?: UriError.InvalidHost.left()

    private fun getPort(match: MatchResult, schemeSpecificPart: String, scheme: String): Either<UriError, Int>? =
      match.groups["port"]?.range?.let { range ->
        val port: Int? = schemeSpecificPart.substring(range).let {
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

    private fun getPathSegmentsOrEmpty(match: MatchResult, schemeSpecificPart: String): Either<UriError, PathSegments> =
      PathSegments.absoluteOrEmptyS(
        match.groups["path"]?.range?.let { range ->
          val pathPart = schemeSpecificPart.substring(range)
          when {
            pathPart.isEmpty() -> emptyList()
            else -> pathPart.removePrefix("/").split("/")
              .map { segment -> segment.decode().fold({ return it.left() }, { it }) }
          }
        } ?: emptyList()
      ).right()

    private fun getQuerySegmentsOrEmpty(
      match: MatchResult,
      schemeSpecificPart: String
    ): Either<UriError, List<QuerySegment>> =
      match.groups["query"]?.range?.let { range ->
        val querySegments: String = schemeSpecificPart.substring(range)
        when (querySegments.contains("&") || querySegments.contains("=")) {
          true -> {
            querySegments.split("&").map { querySegment ->
              querySegment.split("=").map { it.decode(plusAsSpace = true).fold({ return it.left() }, { it }) }
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

    private fun getFragmentSegmentOrNull(
      match: MatchResult,
      schemeSpecificPart: String
    ): Either<UriError, FragmentSegment?> =
      match.groups["fragment"]?.range?.let { range ->
        val fragment = schemeSpecificPart.substring(range)
        when (fragment.isNotEmpty()) {
          true -> FragmentSegment(v = fragment.decode().fold({ return it.left() }, { it }))
          false -> null
        }.right()
      } ?: null.right()
  }

  /** Replace the scheme. Does not validate the new scheme value. */
  fun scheme(s: String): Uri = this.copy(scheme = s)

  /** Replace the user info with a username only. Adds an empty host if one is absent. */
  fun userInfo(username: String): Uri = userInfo(UserInfo(username, null))

  /** Replace the user info with username/password combination. Adds an empty host if one is absent. */
  fun userInfo(username: String, password: String): Uri = userInfo(UserInfo(username, password))

  /** Replace the user info with username/password combination. Adds an empty host if one is absent, and user info
   * is defined.
   */
  fun userInfo(ui: UserInfo?): Uri =
    this.copy(authority = authority?.copy(userInfo = ui) ?: Authority.Empty.userInfo(ui))

  fun userInfo(): UserInfo? = authority?.userInfo

  /** Replace the host. Does not validate the new host value if it's nonempty. */
  fun host(h: String): Uri =
    this.copy(authority = authority?.copy(hostSegment = HostSegment(h)))

  fun host(): String? = authority?.hostSegment?.v

  /** Replace the port. Adds an empty host if one is absent, and port is defined. */
  fun port(p: Int?): Uri =
    this.copy(authority = authority?.copy(port = p) ?: Authority.Empty.port(p))

  fun port(): Int? = authority?.port

  /** Replace the authority. */
  fun authority(a: Authority): Uri =
    this.copy(authority = a)

  fun addPath(p: String, vararg ps: String): Uri =
    addPathSegments(listOf(PathSegment(p)) + ps.map { PathSegment(it) })

  fun addPathSegments(ss: List<PathSegment>): Uri = copy(pathSegments = pathSegments.addSegments(ss))

  fun withPath(p: String, vararg ps: String): Uri =
    withPathSegments(listOf(PathSegment(p)) + ps.map { PathSegment(it) })

  fun withPathSegments(ss: List<PathSegment>): Uri = copy(pathSegments = pathSegments.withSegments(ss))

  /** Replace the whole path with the given one. Leading `/` will be removed, if present, and the path will be
   * split into segments on `/`.
   */
  fun withWholePath(p: String): Uri {
    // removing the leading slash, as it is added during serialization anyway
    val pWithoutLeadingSlash = if (p.startsWith("/")) p.substring(1) else p
    val ps = pWithoutLeadingSlash.split("/", limit = -1)
    return if (ps.isEmpty()) this else withPathSegments(ps.map { PathSegment(it) })
  }

  fun path(): List<String> = pathSegments.segments.map { it.v }

  //

  fun addParam(k: String, v: String?): Uri = v?.let { addParams(listOf(Pair(k, v))) } ?: this

  fun addParams(ps: Map<String, String>): Uri = addParams(ps.toList())

  fun addParams(mqp: QueryParams): Uri =
    this.copy(querySegments = querySegments + QuerySegment.fromQueryParams(mqp))

  fun addParams(ps: List<Pair<String, String>>): Uri =
    this.copy(querySegments = querySegments + ps.map { (k, v) -> QuerySegment.KeyValue(k, v) })

  /** Replace query with the given single optional parameter. */
  fun withParam(k: String, v: String?): Uri = v?.let { withParams(listOf(Pair(k, v))) } ?: this

  /** Replace query with the given parameters. */
  fun withParams(ps: Map<String, String>): Uri = withParams(ps.toList())

  /** Replace query with the given parameters. */
  fun withParams(mqp: QueryParams): Uri =
    this.copy(querySegments = QuerySegment.fromQueryParams(mqp).toList())

  /** Replace query with the given parameters. */
  fun withParams(ps: List<Pair<String, String>>): Uri =
    this.copy(querySegments = ps.map { (k, v) -> QuerySegment.KeyValue(k, v) })

  fun paramsMap(): Map<String, String> = paramsSeq().toMap()

  fun params(): QueryParams {
    val m = linkedMapOf<String, List<String>>() // keeping parameter order
    querySegments.forEach {
      when (it) {
        is QuerySegment.KeyValue -> m[it.k] = m.getOrElse(it.k, { emptyList() }) + listOf(it.v)
        is QuerySegment.Value -> m[it.v] = m.getOrElse(it.v, { emptyList() })
        is QuerySegment.Plain -> m[it.v] = m.getOrElse(it.v, { emptyList() })
      }
    }
    return QueryParams(m.toList())
  }

  fun paramsSeq(): List<Pair<String, String>> = params().toList()

  fun addQuerySegment(qf: QuerySegment): Uri = this.copy(querySegments = querySegments + listOf(qf))

  //

  /** Replace the fragment. */
  fun fragment(f: String?): Uri =
    fragmentSegment(f?.let { FragmentSegment(it) })

  /** Replace the fragment. */
  fun fragmentSegment(s: FragmentSegment?): Uri = this.copy(fragmentSegment = s)

  fun fragment(): String? = fragmentSegment?.v

  //

  fun toJavaUri(): URI = URI(toString())

  suspend fun resolveOrNull(other: Uri): Uri? = Uri(toJavaUri().resolve(other.toJavaUri()))

  //

  fun hostSegmentEncoding(encoding: Encoding): Uri =
    copy(authority = authority?.copy(hostSegment = authority.hostSegment.encoding(encoding)))

  fun pathSegmentsEncoding(encoding: Encoding): Uri =
    copy(
      pathSegments = when (pathSegments) {
        is PathSegments.EmptyPath -> PathSegments.EmptyPath
        is PathSegments.AbsolutePath -> PathSegments.AbsolutePath(pathSegments.segments.map { it.encoding(encoding) })
        is PathSegments.RelativePath -> PathSegments.RelativePath(pathSegments.segments.map { it.encoding(encoding) })
      }
    )

  /** Replace encoding for query segments: applies to key-value, only-value and plain ones. */
  fun querySegmentsEncoding(encoding: Encoding): Uri =
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
  fun queryValueSegmentsEncoding(valueEncoding: Encoding): Uri =
    copy(
      querySegments = querySegments.map {
        when (it) {
          is QuerySegment.KeyValue -> QuerySegment.KeyValue(it.k, it.v, it.keyEncoding, valueEncoding)
          is QuerySegment.Value -> QuerySegment.Value(it.v, valueEncoding)
          is QuerySegment.Plain -> QuerySegment.Plain(it.v, valueEncoding)
        }
      }
    )

  fun fragmentSegmentEncoding(encoding: Encoding): Uri =
    copy(fragmentSegment = fragmentSegment?.encoding(encoding))

  override fun toString(): String {
    fun StringBuilder.encodeQuerySegments(qss: List<QuerySegment>, previousWasPlain: Boolean): String =
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
  public data class UnexpectedScheme(val errorMessage: String) : UriError
  public data class CantParse(val errorMessage: String) : UriError
  object InvalidHost : UriError
  object InvalidPort : UriError
  public data class IllegalArgument(val errorMessage: String) : UriError
}

public data class Authority(val userInfo: UserInfo?, val hostSegment: HostSegment, val port: Int?) {

  public companion object {

    val Empty: Authority = Authority(null, HostSegment(""), null)

    operator fun invoke(host: String): Authority =
      Authority(null, HostSegment(host), null)

    operator fun invoke(userInfo: UserInfo?, host: String, port: Int?): Authority =
      Authority(userInfo, HostSegment(host), port)
  }

  /** Replace the user info with a username only. */
  fun userInfo(username: String): Authority = this.copy(userInfo = UserInfo(username, null))

  /** Replace the user info with username/password combination. */
  fun userInfo(username: String, password: String): Authority =
    this.copy(userInfo = UserInfo(username, password))

  /** Replace the user info. */
  fun userInfo(ui: UserInfo?): Authority = this.copy(userInfo = ui)

  /** Replace the host. Does not validate the new host value if it's nonempty. */
  fun host(h: String): Authority = this.copy(hostSegment = HostSegment(h))

  fun host(): String = hostSegment.v

  /** Replace the port. */
  fun port(p: Int?): Authority = this.copy(port = p)

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

typealias Encoding = (String) -> String

sealed class Segment(
  open val v: String,
  open val encoding: Encoding
) {
  fun encoded(): String = encoding(v)
  abstract fun encoding(e: Encoding): Segment
}

public data class HostSegment(
  override val v: String,
  override val encoding: Encoding = Standard
) : Segment(v, encoding) {

  public companion object {
    private val IpV6Pattern = "[0-9a-fA-F:]+".toRegex()
    val Standard: Encoding = { s ->
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
    val Standard: Encoding = {
      it.encode(Rfc3986.PathSegment)
    }
  }

  override fun encoding(e: Encoding): PathSegment = copy(encoding = e)
}

public sealed interface PathSegments {

  val segments: List<PathSegment>

  public companion object {
    fun absoluteOrEmptyS(segments: List<String>): PathSegments = absoluteOrEmpty(segments.map { PathSegment(it) })

    fun absoluteOrEmpty(segments: List<PathSegment>): PathSegments =
      if (segments.isEmpty()) EmptyPath else AbsolutePath(segments)
  }

  fun add(p: String, vararg ps: String): PathSegments = add(listOf(p) + ps)
  fun add(ps: List<String>): PathSegments = addSegments(ps.map { PathSegment(it) })
  fun addSegment(s: PathSegment): PathSegments = addSegments(listOf(s))
  fun addSegments(s1: PathSegment, s2: PathSegment, ss: List<PathSegment>): PathSegments =
    addSegments(listOf(s1, s2) + ss)

  fun addSegments(ss: List<PathSegment>): PathSegments {
    val base = if (segments.lastOrNull()?.v?.isEmpty() == true) emptyList() else segments
    return withSegments(base + ss)
  }

  fun withS(p: String, ps: Sequence<String>): PathSegments = withS(listOf(p) + ps)
  fun withS(ps: List<String>): PathSegments = withSegments(ps.map { PathSegment(it) })

  fun withSegment(s: PathSegment): PathSegments = withSegments(listOf(s))
  fun withSegments(s1: PathSegment, s2: PathSegment, ss: List<PathSegment>): PathSegments =
    withSegments(listOf(s1, s2) + ss)

  fun withSegments(ss: List<PathSegment>): PathSegments

  object EmptyPath : PathSegments {
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
    /** Encodes all reserved characters using [[java.net.URLEncoder.encode()]]. */
    val All: Encoding = {
      UriCompatibility.encodeQuery(it, "UTF-8")
    }

    /** Encodes only the `&` and `=` reserved characters, which are usually used to separate query parameter names and
     * values.
     */
    val Standard: Encoding = {
      it.encode(allowedCharacters = Rfc3986.Query - setOf('&', '='), spaceAsPlus = true, encodePlus = true)
    }

    /** Encodes only the `&` reserved character, which is usually used to separate query parameter names and values.
     * The '=' sign is allowed in values.
     */
    val StandardValue: Encoding = {
      it.encode(Rfc3986.Query - setOf('&'), spaceAsPlus = true, encodePlus = true)
    }

    /** Doesn't encode any of the reserved characters, leaving intact all
     * characters allowed in the query string as defined by RFC3986.
     */
    val Relaxed: Encoding = {
      it.encode(Rfc3986.Query, spaceAsPlus = true)
    }

    /** Doesn't encode any of the reserved characters, leaving intact all
     * characters allowed in the query string as defined by RFC3986 as well
     * as the characters `[` and `]`. These brackets aren't legal in the
     * query part of the URI, but some servers use them unencoded. See
     * https://stackoverflow.com/questions/11490326/is-array-syntax-using-square-brackets-in-url-query-strings-valid
     * for discussion.
     */
    val RelaxedWithBrackets: Encoding = {
      it.encode(Rfc3986.SegmentWithBrackets, spaceAsPlus = true)
    }

    fun fromQueryParams(mqp: QueryParams): Iterable<QuerySegment> =
      mqp.toMultiList().flatMap { (k: String, vs: List<String>) ->
        when {
          vs.isEmpty() -> listOf(Value(k))
          else -> vs.map { v -> KeyValue(k, v) }
        }
      }
  }

  /**
   * @param keyEncoding See [[Plain.encoding]]
   * @param valueEncoding See [[Plain.encoding]]
   */
  public data class KeyValue(
    val k: String,
    val v: String,
    val keyEncoding: Encoding = Standard,
    val valueEncoding: Encoding = Standard
  ) : QuerySegment {
    override fun toString() = "KeyValue($k, $v)"
  }

  /** A query fragment which contains only the value, without a key. */
  public data class Value(
    val v: String,
    val encoding: Encoding = StandardValue
  ) : QuerySegment {
    override fun toString() = "Value($v)"
  }

  /** A query fragment which will be inserted into the query, without and
   * preceding or following separators. Allows constructing query strings
   * which are not (only) &-separated key-value pairs.
   *
   * @param encoding How to encode the value, and which characters should be escaped. The RFC3986 standard
   * defines that the query can include these special characters, without escaping:
   * {{{
   * /?:@-._~!$&()*+,;=
   * }}}
   * See:
   * [[https://stackoverflow.com/questions/2322764/what-characters-must-be-escaped-in-an-http-query-string]]
   * [[https://stackoverflow.com/questions/2366260/whats-valid-and-whats-not-in-a-uri-query]]
   */
  public data class Plain(
    val v: String,
    val encoding: Encoding = StandardValue
  ) : QuerySegment {
    override fun toString() = "Plain($v)"
  }
}

public data class FragmentSegment(
  override val v: String,
  override val encoding: Encoding = Standard
) : Segment(v, encoding) {

  public companion object {
    val Standard: Encoding = {
      it.encode(Rfc3986.Fragment)
    }

    val RelaxedWithBrackets: Encoding = {
      it.encode(Rfc3986.SegmentWithBrackets, spaceAsPlus = true)
    }
  }

  override fun encoding(e: Encoding): FragmentSegment = copy(encoding = e)
}
