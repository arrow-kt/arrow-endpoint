@file:Suppress("MemberVisibilityCanBePrivate")

package arrow.endpoint.model

import arrow.endpoint.model.Rfc3986.encode

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
