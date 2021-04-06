package com.fortysevendegrees.thool.model

data class UserInfo(val username: String, val password: String?)

/**
 * A [[https://en.wikipedia.org/wiki/Uniform_Resource_Identifier URI]]. Can represent both relative and absolute
 * URIs, hence in terms of [[https://tools.ietf.org/html/rfc3986]], this is a URI reference.
 *
 * All components (scheme, host, query, ...) are stored decoded, and become encoded upon serialization
 * (using [[toString]]).
 *
 * Instances can be created using the uri interpolator: `uri"..."` (see [[UriInterpolator]]), or the factory methods
 * on the [[Uri]] companion object.
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
data class Uri(
  val scheme: String?,
  val authority: Authority?,
  val pathSegments: PathSegments,
  val querySegments: List<QuerySegment>,
  val fragmentSegment: Segment?
) {
//
//  /** Replace the scheme. Does not validate the new scheme value. */
//  fun scheme(s: String?): Uri = this.copy(scheme = s)
//
//  /** Replace the user info with a username only. Adds an empty host if one is absent. */
//  fun userInfo(username: String): Uri = userInfo(UserInfo(username, null))
//
//  /** Replace the user info with username/password combination. Adds an empty host if one is absent. */
//  fun userInfo(username: String, password: String): Uri = userInfo(UserInfo(username, password))
//
//  /** Replace the user info with username/password combination. Adds an empty host if one is absent, and user info
//   * is defined.
//   */
//  fun userInfo(ui: UserInfo?): Uri = TODO()
////    ui match
////  {
////    case Some (v) => this.copy(authority = Some(authority.getOrElse(Authority.Empty).userInfo(Some(v))))
////    case None => this.copy(authority = authority.com.fortysevendegrees.thool.map(_.userInfo(None)))
////  }
//
//  fun userInfo(): UserInfo? =  TODO()//authority.flatMap(_.userInfo)
//
//  /** Replace the host. Does not validate the new host value if it's nonempty. */
//  fun host(h: String): Uri = hostSegment(HostSegment(h))
//
//  /** Replace the host. Does not validate the new host value if it's nonempty. */
//  fun hostSegment(s: Segment): Uri = hostSegment(Some(s))
//
//  /** Replace the host. Does not validate the new host value if it's nonempty. */
//  fun hostSegment(s: Option[Segment]): Uri = TODO()
////    this.copy(authority = authority match
////  {
////    case Some (a) => s.com.fortysevendegrees.thool.map(a.hostSegment(_))
////    case None => s . com.fortysevendegrees.thool.map (Authority(None, _, None))
////  })
//
//  fun host: Option[String] = authority.com.fortysevendegrees.thool.map(_.hostSegment.v)
//
//  /** Replace the port. Adds an empty host if one is absent. */
//  fun port(p: Int): Uri = port(Some(p))
//
//  /** Replace the port. Adds an empty host if one is absent, and port is defined. */
//  fun port(p: Option[Int]): Uri = TODO()
////    p match  {
////    case Some (v) => this.copy(authority = Some(authority.getOrElse(Authority.Empty).port(v)))
////    case None => this.copy(authority = authority.com.fortysevendegrees.thool.map(_.port(None)))
////  }
//
//  fun port: Option[Int] = authority.flatMap(_.port)
//
//  /** Replace the authority. */
//  fun authority(a: Authority): Uri = this.copy(authority = Some(a))
//
//  /** Replace the authority. */
//  fun authority(a: Some[Authority]): Uri = this.copy(authority = a)
//
//  fun addPath(p: String, ps: String*): Uri = addPath(p :: ps.toList)
//  fun addPath(ps: scala.collection.Seq[String]): Uri = addPathSegments(ps.toList.com.fortysevendegrees.thool.map(PathSegment(_)))
//  fun addPathSegment(s: Segment): Uri = addPathSegments(List(s))
//  fun addPathSegments(s1: Segment, s2: Segment, ss: Segment*): Uri = addPathSegments(s1 :: s2 :: ss.toList)
//  fun addPathSegments(ss: scala.collection.Seq[Segment]): Uri = copy(pathSegments = pathSegments.addSegments(ss))
//
//  fun withPath(p: String, ps: String*): Uri = withPath(p :: ps.toList)
//  fun withPath(ps: scala.collection.Seq[String]): Uri = withPathSegments(ps.toList.com.fortysevendegrees.thool.map(PathSegment(_)))
//  fun withPathSegment(s: Segment): Uri = withPathSegments(List(s))
//  fun withPathSegments(s1: Segment, s2: Segment, ss: Segment*): Uri = withPathSegments(s1 :: s2 :: ss.toList)
//  fun withPathSegments(ss: scala.collection.Seq[Segment]): Uri = copy(pathSegments = pathSegments.withSegments(ss))
//
//  /** Replace the whole path with the given one. Leading `/` will be removed, if present, and the path will be
//   * split into segments on `/`.
//   */
//  fun withWholePath(p: String): Uri {
//    // removing the leading slash, as it is added during serialization anyway
//    val pWithoutLeadingSlash = if (p.startsWith("/")) p.substring(1) else p
//    val ps = pWithoutLeadingSlash.split("/", -1).toList
//    withPath(ps)
//  }
//
//  fun path: Seq[String] = pathSegments.segments.com.fortysevendegrees.thool.map(_.v).toList
//
//  //
//
//  fun addParam(k: String, v: String): Uri = addParams(k -> v)
//  fun addParam(k: String, v: Option[String]): Uri = v.com.fortysevendegrees.thool.map(addParam(k, _)).getOrElse(this)
//  fun addParams(ps: Map[String, String]): Uri = addParams(ps.toSeq: _*)
//  fun addParams(mqp: QueryParams): Uri =
//  {
//    this.copy(querySegments = querySegments++ QuerySegment . fromQueryParams (mqp))
//  }
//  fun addParams(ps: (String, String)*): Uri =
//  {
//    this.copy(querySegments = querySegments++ ps . com.fortysevendegrees.thool.map {
//      case(k, v) =>
//      KeyValue(k, v)
//    })
//  }
//
//  /** Replace query with the given single parameter. */
//  fun withParam(k: String, v: String): Uri = withParams(k -> v)
//
//  /** Replace query with the given single optional parameter. */
//  fun withParam(k: String, v: Option[String]): Uri = v.com.fortysevendegrees.thool.map(withParam(k, _)).getOrElse(this)
//
//  /** Replace query with the given parameters. */
//  fun withParams(ps: Map[String, String]): Uri = withParams(ps.toSeq: _*)
//
//  /** Replace query with the given parameters. */
//  fun withParams(mqp: QueryParams): Uri = this.copy(querySegments = QuerySegment.fromQueryParams(mqp).toList)
//
//  /** Replace query with the given parameters. */
//  fun withParams(ps: (String, String)*): Uri = this.copy(querySegments = ps.com.fortysevendegrees.thool.map
//  {
//    case(k, v) =>
//    KeyValue(k, v)
//  }.toList)
//
//  fun paramsMap: Map[String, String] = paramsSeq.toMap
//
//  fun params: QueryParams = QueryParams.fromSeq(paramsSeq)
//
//  fun paramsSeq: Seq[(String, String)] =
//  querySegments.collect
//  {
//    case KeyValue (k, v, _, _) =>
//    k -> v
//  }
//
//  fun addQuerySegment(qf: QuerySegment): Uri = this.copy(querySegments = querySegments :+ qf)
//
//  //
//
//  /** Replace the fragment. */
//  fun fragment(f: String): Uri = fragment(Some(f))
//
//  /** Replace the fragment. */
//  fun fragment(f: Option[String]): Uri = fragmentSegment(f.com.fortysevendegrees.thool.map(FragmentSegment(_)))
//
//  /** Replace the fragment. */
//  fun fragmentSegment(s: Option[Segment]): Uri = this.copy(fragmentSegment = s)
//
//  fun fragment: Option[String] = fragmentSegment.com.fortysevendegrees.thool.map(_.v)
//
//  //
//
//  fun toJavaUri: URI = new URI(toString())
//
//  fun isAbsolute: Boolean = scheme.isDefined
//  fun isRelative: Boolean = !isAbsolute
//
//  fun resolve(other: Uri): Uri = Uri(toJavaUri.resolve(other.toJavaUri))
//
//  //
//
//  fun hostSegmentEncoding(encoding: Encoding): Uri =
//  copy(authority = authority.com.fortysevendegrees.thool.map(a => a.copy(hostSegment = a.hostSegment.encoding(encoding))))
//
//  fun pathSegmentsEncoding(encoding: Encoding): Uri = copy(pathSegments = pathSegments match
//  {
//    case Uri . EmptyPath => Uri . EmptyPath
//      case Uri . AbsolutePath (segments) => Uri.AbsolutePath(segments.com.fortysevendegrees.thool.map(_.encoding(encoding)))
//    case Uri . RelativePath (segments) => Uri.RelativePath(segments.com.fortysevendegrees.thool.map(_.encoding(encoding)))
//  })
//
//  /** Replace encoding for query segments: applies to key-value, only-value and plain ones. */
//  fun querySegmentsEncoding(encoding: Encoding): Uri = copy(querySegments = querySegments.com.fortysevendegrees.thool.map
//  {
//    case KeyValue (k, v, _, _) => KeyValue(k, v, encoding, encoding)
//    case Value (v, _)          => Value(v, encoding)
//    case Plain (v, _)          => Plain(v, encoding)
//  })
//
//  /** Replace encoding for the value part of key-value query segments and for only-value ones. */
//  fun queryValueSegmentsEncoding(valueEncoding: Encoding): Uri = copy(querySegments = querySegments.com.fortysevendegrees.thool.map
//  {
//    case KeyValue (k, v, keyEncoding, _) => KeyValue(k, v, keyEncoding, valueEncoding)
//    case Value (v, _)                    => Value(v, valueEncoding)
//    case s => s
//  })
//
//  fun fragmentSegmentEncoding(encoding: Encoding): Uri =
//  copy(fragmentSegment = fragmentSegment.com.fortysevendegrees.thool.map(f => f.encoding(encoding)))
//
//  override fun toString: String =
//  {
//    @tailrec
//    fun encodeQuerySegments (qss: List[QuerySegment], previousWasPlain: Boolean, sb: StringBuilder): String =
//    qss match {
//      case Nil => sb . toString ()
//
//      case Plain (v, enc)::t =>
//      encodeQuerySegments(t, previousWasPlain = true, sb.append(enc(v)))
//
//      case Value (v, enc)::t =>
//      if (!previousWasPlain) sb.append("&")
//      sb.append(enc(v))
//      encodeQuerySegments(t, previousWasPlain = false, sb)
//
//      case KeyValue (k, v, kEnc, vEnc)::t =>
//      if (!previousWasPlain) sb.append("&")
//      sb.append(kEnc(k)).append("=").append(vEnc(v))
//      encodeQuerySegments(t, previousWasPlain = false, sb)
//    }
//
//    val schemeS = scheme.com.fortysevendegrees.thool.map(s => encode (Rfc3986.Scheme)(s) + ":").getOrElse("")
//    val authorityS = authority.fold("")(_.toString)
//    val pathPrefixS = pathSegments match {
//      case _ if authority.isEmpty && scheme.isDefined => ""
//      case Uri . EmptyPath => ""
//      case Uri . AbsolutePath (_)                        => "/"
//      case Uri . RelativePath (_)                        => ""
//    }
//    val pathS = pathSegments.segments.com.fortysevendegrees.thool.map(_.encoded).mkString("/")
//    val queryPrefixS = if (querySegments.isEmpty) "" else "?"
//
//    val queryS = encodeQuerySegments(querySegments.toList, previousWasPlain = true, new StringBuilder ())
//
//    // https://stackoverflow.com/questions/2053132/is-a-colon-safe-for-friendly-url-use/2053640#2053640
//    val fragS = fragmentSegment.fold("")(s => "#"+s.encoded)
//
//    s"$schemeS$authorityS$pathPrefixS$pathS$queryPrefixS$queryS$fragS"
//  }
//}
//
///** For a general description of the behavior of `apply`, `parse`, `safeApply` and `unsafeApply` methods, see [[sttp.model]].
// *
// * The `safeApply` methods return a validation error if the scheme contains illegal characters or if the host is empty.
// */
//object Uri extends UriInterpolator {
//  private val AllowedSchemeCharacters = "[a-zA-Z][a-zA-Z0-9+-.]*".r
//  private fun validateHost(host: Option[String]): Option[String] =
//  host.flatMap(h => if (h.isEmpty) Some("Host cannot be empty") else None)
//  private fun validateScheme(scheme: Option[String]) = scheme.flatMap {
//    s =>
//    if (AllowedSchemeCharacters.unapplySeq(s).isEmpty)
//      Some("Scheme can only contain alphanumeric characters, +, - and .")
//    else None
//  }
//
//  fun safeApply (host: String): Either[String, Uri] =
//  safeApply("http", Some(Authority(host)), Vector.empty, Vector.empty, None)
//  fun safeApply (host: String, port: Int): Either[String, Uri] =
//  safeApply("http", Some(Authority(host, port)), Vector.empty, Vector.empty, None)
//  fun safeApply (host: String, port: Int, path: Seq[String]): Either[String, Uri] =
//  safeApply("http", Some(Authority(host, port)), path.com.fortysevendegrees.thool.map(PathSegment(_)), Vector.empty, None)
//  fun safeApply (scheme: String, path: Seq[String]): Either[String, Uri] =
//  safeApply(scheme, None, path.com.fortysevendegrees.thool.map(PathSegment(_)), Vector.empty, None)
//  fun safeApply (scheme: String, host: String): Either[String, Uri] =
//  safeApply(scheme, Some(Authority(host)), Vector.empty, Vector.empty, None)
//  fun safeApply (scheme: String, host: String, port: Int): Either[String, Uri] =
//  safeApply(scheme, Some(Authority(host, port)), Vector.empty, Vector.empty, None)
//  fun safeApply (scheme: String, host: String, port: Int, path: Seq[String]): Either[String, Uri] =
//  safeApply(scheme, Some(Authority(host, port)), path.com.fortysevendegrees.thool.map(PathSegment(_)), Vector.empty, None)
//  fun safeApply (scheme: String, host: String, path: Seq[String]): Either[String, Uri] =
//  safeApply(scheme, Some(Authority(host)), path.com.fortysevendegrees.thool.map(PathSegment(_)), Vector.empty, None)
//  fun safeApply (scheme: String, host: String, path: Seq[String], fragment: Option[String]): Either[String, Uri] =
//  safeApply(
//    scheme,
//    Some(Authority(host)),
//    path.com.fortysevendegrees.thool.map(PathSegment(_)),
//    Vector.empty,
//    fragment.com.fortysevendegrees.thool.map(FragmentSegment(_))
//  )
//  fun safeApply (
//    scheme: String,
//  userInfo: Option[UserInfo],
//  host: String,
//  port: Option[Int],
//  path: Seq[String],
//  querySegments: Seq[QuerySegment],
//  fragment: Option[String]
//  ): Either[String, Uri] =
//  safeApply(
//    scheme,
//    Some(Authority(userInfo, HostSegment(host), port)),
//    path.com.fortysevendegrees.thool.map(PathSegment(_)),
//    querySegments,
//    fragment.com.fortysevendegrees.thool.map(FragmentSegment(_))
//  )
//  fun safeApply (
//    scheme: String,
//  authority: Option[Authority],
//  pathSegments: Seq[Segment],
//  querySegments: Seq[QuerySegment],
//  fragmentSegment: Option[Segment]
//  ): Either[String, Uri] =
//  Validate.all(validateScheme(Some(scheme)), validateHost(authority.com.fortysevendegrees.thool.map(_.hostSegment.v)))(
//    apply(
//      Some(scheme),
//      authority,
//      PathSegments.absoluteOrEmpty(pathSegments),
//      querySegments,
//      fragmentSegment
//    )
//  )
//
//  //
//
//  fun unsafeApply (host: String): Uri =
//  unsafeApply("http", Some(Authority(host)), Vector.empty, Vector.empty, None)
//  fun unsafeApply (host: String, port: Int): Uri =
//  unsafeApply("http", Some(Authority(host, port)), Vector.empty, Vector.empty, None)
//  fun unsafeApply (host: String, port: Int, path: Seq[String]): Uri =
//  unsafeApply("http", Some(Authority(host, port)), path.com.fortysevendegrees.thool.map(PathSegment(_)), Vector.empty, None)
//  fun unsafeApply (scheme: String, path: Seq[String]): Uri =
//  unsafeApply(scheme, None, path.com.fortysevendegrees.thool.map(PathSegment(_)), Vector.empty, None)
//  fun unsafeApply (scheme: String, host: String): Uri =
//  unsafeApply(scheme, Some(Authority(host)), Vector.empty, Vector.empty, None)
//  fun unsafeApply (scheme: String, host: String, port: Int): Uri =
//  unsafeApply(scheme, Some(Authority(host, port)), Vector.empty, Vector.empty, None)
//  fun unsafeApply (scheme: String, host: String, port: Int, path: Seq[String]): Uri =
//  unsafeApply(scheme, Some(Authority(host, port)), path.com.fortysevendegrees.thool.map(PathSegment(_)), Vector.empty, None)
//  fun unsafeApply (scheme: String, host: String, path: Seq[String]): Uri =
//  unsafeApply(scheme, Some(Authority(host)), path.com.fortysevendegrees.thool.map(PathSegment(_)), Vector.empty, None)
//  fun unsafeApply (scheme: String, host: String, path: Seq[String], fragment: Option[String]): Uri =
//  unsafeApply(
//    scheme,
//    Some(Authority(host)),
//    path.com.fortysevendegrees.thool.map(PathSegment(_)),
//    Vector.empty,
//    fragment.com.fortysevendegrees.thool.map(FragmentSegment(_))
//  )
//  fun unsafeApply (
//    scheme: String,
//  userInfo: Option[UserInfo],
//  host: String,
//  port: Option[Int],
//  path: Seq[String],
//  querySegments: Seq[QuerySegment],
//  fragment: Option[String]
//  ): Uri =
//  unsafeApply(
//    scheme,
//    Some(Authority(userInfo, HostSegment(host), port)),
//    path.com.fortysevendegrees.thool.map(PathSegment(_)),
//    querySegments,
//    fragment.com.fortysevendegrees.thool.map(FragmentSegment(_))
//  )
//  fun unsafeApply (
//    scheme: String,
//  authority: Option[Authority],
//  pathSegments: Seq[Segment],
//  querySegments: Seq[QuerySegment],
//  fragmentSegment: Option[Segment]
//  ): Uri =
//  safeApply(scheme, authority, pathSegments, querySegments, fragmentSegment).getOrThrow
//
//  //
//
//  fun apply (host: String): Uri =
//  apply(Some("http"), Some(Authority(host)), EmptyPath, Vector.empty, None)
//  fun apply (host: String, port: Int): Uri =
//  apply(Some("http"), Some(Authority(host, port)), EmptyPath, Vector.empty, None)
//  fun apply (host: String, port: Int, path: Seq[String]): Uri =
//  apply(Some("http"), Some(Authority(host, port)), PathSegments.absoluteOrEmptyS(path), Vector.empty, None)
//  fun apply (scheme: String, path: Seq[String]): Uri =
//  apply(Some(scheme), None, PathSegments.absoluteOrEmptyS(path), Vector.empty, None)
//  fun apply (scheme: String, host: String): Uri =
//  apply(Some(scheme), Some(Authority(host)), EmptyPath, Vector.empty, None)
//  fun apply (scheme: String, host: String, port: Int): Uri =
//  apply(Some(scheme), Some(Authority(host, port)), EmptyPath, Vector.empty, None)
//  fun apply (scheme: String, host: String, port: Int, path: Seq[String]): Uri =
//  apply(Some(scheme), Some(Authority(host, port)), PathSegments.absoluteOrEmptyS(path), Vector.empty, None)
//  fun apply (scheme: String, host: String, path: Seq[String]): Uri =
//  apply(Some(scheme), Some(Authority(host)), PathSegments.absoluteOrEmptyS(path), Vector.empty, None)
//  fun apply (scheme: String, host: String, path: Seq[String], fragment: Option[String]): Uri =
//  apply(
//    Some(scheme),
//    Some(Authority(host)),
//    PathSegments.absoluteOrEmptyS(path),
//    Vector.empty,
//    fragment.com.fortysevendegrees.thool.map(FragmentSegment(_))
//  )
//  fun apply (
//    scheme: String,
//  userInfo: Option[UserInfo],
//  host: String,
//  port: Option[Int],
//  path: Seq[String],
//  querySegments: Seq[QuerySegment],
//  fragment: Option[String]
//  ): Uri = {
//    apply(
//      Some(scheme),
//      Some(Authority(userInfo, HostSegment(host), port)),
//      PathSegments.absoluteOrEmptyS(path),
//      querySegments,
//      fragment.com.fortysevendegrees.thool.map(FragmentSegment(_))
//    )
//  }
//  fun apply (
//    scheme: String,
//  authority: Option[Authority],
//  path: Seq[Segment],
//  querySegments: Seq[QuerySegment],
//  fragment: Option[Segment]
//  ): Uri = {
//    apply(
//      Some(scheme),
//      authority,
//      PathSegments.absoluteOrEmpty(path),
//      querySegments,
//      fragment
//    )
//  }
//
//  //
//
//  /** Create a relative URI with an absolute path. */
//  fun relative (path: Seq[String]): Uri = relative(path, Vector.empty, None)
//
//  /** Create a relative URI with an absolute path. */
//  fun relative (path: Seq[String], fragment: Option[String]): Uri = relative(path, Vector.empty, fragment)
//
//  /** Create a relative URI with an absolute path. */
//  fun relative (path: Seq[String], querySegments: Seq[QuerySegment], fragment: Option[String]): Uri =
//  apply(None, None, PathSegments.absoluteOrEmptyS(path), querySegments, fragment.com.fortysevendegrees.thool.map(FragmentSegment(_)))
//
//  /** Create a relative URI with a relative path. */
//  fun pathRelative (path: Seq[String]): Uri = pathRelative(path, Vector.empty, None)
//
//  /** Create a relative URI with a relative path. */
//  fun pathRelative (path: Seq[String], fragment: Option[String]): Uri = pathRelative(path, Vector.empty, fragment)
//
//  /** Create a relative URI with a relative path. */
//  fun pathRelative (path: Seq[String], querySegments: Seq[QuerySegment], fragment: Option[String]): Uri =
//  apply(None, None, RelativePath(path.com.fortysevendegrees.thool.map(PathSegment(_))), querySegments, fragment.com.fortysevendegrees.thool.map(FragmentSegment(_)))
//
//  //
//
//  fun apply (javaUri: URI): Uri = uri"${javaUri.toString}"
//
//  fun parse (uri: String): Either[String, Uri] =
//  Try(uri"$uri") match {
//    case Success (u)            => Right(u)
//    case Failure (e: Exception) => Left(e.getMessage)
//    case Failure (t: Throwable) => throw t
//  }
//
//  fun unsafeParse (uri: String): Uri = uri"$uri"
//
//  //
//  //

//
//    object HostEncoding {
//      private val IpV6Pattern = "[0-9a-fA-F:]+".r
//
//      val Standard: Encoding = {
//        case s @ IpV6Pattern() if s.count(_ == ':') >= 2 => s"[$s]"
//        case s => UriCompatibility . encodeDNSHost (s)
//      }
//    }
//
//  object PathSegmentEncoding {
//    val Standard: Encoding = encode(Rfc3986.PathSegment)
//  }
//
//  object QuerySegmentEncoding {
//
//    /** Encodes all reserved characters using [[java.net.URLEncoder.encode()]]. */
//    val All: Encoding = UriCompatibility.encodeQuery(_, "UTF-8")
//
//    /** Encodes only the `&` and `=` reserved characters, which are usually used to separate query parameter names and
//     * values.
//     */
//    val Standard: Encoding = encode(Rfc3986.Query-- Set ('&', '='), spaceAsPlus = true, encodePlus = true)
//
//    /** Encodes only the `&` reserved character, which is usually used to separate query parameter names and values.
//     * The '=' sign is allowed in values.
//     */
//    val StandardValue: Encoding = encode(Rfc3986.Query-- Set ('&'), spaceAsPlus = true, encodePlus = true)
//
//    /** Doesn't encode any of the reserved characters, leaving intact all
//     * characters allowed in the query string as defined by RFC3986.
//     */
//    val Relaxed: Encoding = encode(Rfc3986.Query, spaceAsPlus = true)
//
//    /** Doesn't encode any of the reserved characters, leaving intact all
//     * characters allowed in the query string as defined by RFC3986 as well
//     * as the characters `[` and `]`. These brackets aren't legal in the
//     * query part of the URI, but some servers use them unencoded. See
//     * https://stackoverflow.com/questions/11490326/is-array-syntax-using-square-brackets-in-url-query-strings-valid
//     * for discussion.
//     */
//    val RelaxedWithBrackets: Encoding = encode(Rfc3986.QueryWithBrackets, spaceAsPlus = true)
//  }
//
//  object FragmentEncoding {
//    val Standard: Encoding = encode(Rfc3986.Fragment)
//  }

}

typealias Encoding = (String) -> String

data class Segment(val v: String, val encoding: Encoding) {
  fun encoded(): String = encoding(v)
  fun encoding(e: Encoding): Segment = copy(encoding = e)
}

//  object HostSegment {
//    fun apply(v: String): Segment = Segment(v, HostEncoding.Standard)
//  }
//
sealed interface PathSegments {
//    fun segments : collection . Seq [Segment]
//
//    fun add (p: String, ps: String*): PathSegments = add(p::ps.toList)
//    fun add (ps: scala.collection.Seq[String]): PathSegments = addSegments(ps.toList.com.fortysevendegrees.thool.map(PathSegment(_)))
//    fun addSegment (s: Segment): PathSegments = addSegments(List(s))
//    fun addSegments (s1: Segment, s2: Segment, ss: Segment*): PathSegments = addSegments(s1::s2::ss.toList)
//    fun addSegments (ss: scala.collection.Seq[Segment]): PathSegments = {
//    val base = if (segments.lastOption.exists(_.v.isEmpty)) segments.init else segments
//    withSegments(base++ ss . toList)
//  }
//
//    fun withS (p: String, ps: String*): PathSegments = withS(p::ps.toList)
//    fun withS (ps: scala.collection.Seq[String]): PathSegments = withSegments(ps.toList.com.fortysevendegrees.thool.map(PathSegment(_)))
//    fun withSegment (s: Segment): PathSegments = withSegments(List(s))
//    fun withSegments (s1: Segment, s2: Segment, ss: Segment*): PathSegments = withSegments(s1::s2::ss.toList)
//    fun withSegments (ss: scala.collection.Seq[Segment]): PathSegments
//  }
//  object PathSegments {
//    fun absoluteOrEmptyS(segments: Seq[String]): PathSegments = absoluteOrEmpty(segments.com.fortysevendegrees.thool.map(PathSegment(_)))
//    fun absoluteOrEmpty(segments: Seq[Segment]): PathSegments =
//    if (segments.isEmpty) EmptyPath else AbsolutePath(segments)
//  }
//  case object EmptyPath extends PathSegments {
//    override fun withSegments(ss: collection. Seq [Segment]): PathSegments = AbsolutePath(ss.toList)
//    override fun segments: collection.Seq[Segment] = Nil
//    override fun toString: String = ""
//  }
//  case class AbsolutePath(segments: Seq[Segment]) extends PathSegments {
//    override fun withSegments(ss: scala. collection . Seq [Segment]): AbsolutePath = copy(segments = ss.toList)
//    override fun toString: String = "/"+segments.com.fortysevendegrees.thool.map(_.encoded).mkString("/")
//  }
//  case class RelativePath(segments: Seq[Segment]) extends PathSegments {
//    override fun withSegments(ss: scala. collection . Seq [Segment]): RelativePath = copy(segments = ss.toList)
//    override fun toString: String = segments.com.fortysevendegrees.thool.map(_.encoded).mkString("/")
//  }
//
//  object PathSegment {
//    fun apply(v: String): Segment = Segment(v, PathSegmentEncoding.Standard)
//  }
//
//  object FragmentSegment {
//    fun apply(v: String): Segment = Segment(v, FragmentEncoding.Standard)
//  }
}
  sealed interface QuerySegment
//  object QuerySegment {
//
//    /** @param keyEncoding See [[Plain.encoding]]
//     * @param valueEncoding See [[Plain.encoding]]
//     */
//    case
//    class KeyValue(
//      k: String,
//      v: String,
//      keyEncoding: Encoding = QuerySegmentEncoding.Standard,
//      valueEncoding: Encoding = QuerySegmentEncoding.Standard
//    ) extends QuerySegment
//    {
//      override fun toString = s"KeyValue($k,$v,[keyEncoding],[valueEncoding])"
//    }
//
//    /** A query fragment which contains only the value, without a key. */
//    case
//    class Value(v: String, encoding: Encoding = QuerySegmentEncoding.StandardValue) extends QuerySegment
//    {
//      override fun toString = s"Value($v,[encoding])"
//    }
//
//    /** A query fragment which will be inserted into the query, without and
//     * preceding or following separators. Allows constructing query strings
//     * which are not (only) &-separated key-value pairs.
//     *
//     * @param encoding How to encode the value, and which characters should be escaped. The RFC3986 standard
//     * defines that the query can include these special characters, without escaping:
//     * {{{
//     * /?:@-._~!$&()*+,;=
//     * }}}
//     * See:
//     * [[https://stackoverflow.com/questions/2322764/what-characters-must-be-escaped-in-an-http-query-string]]
//     * [[https://stackoverflow.com/questions/2366260/whats-valid-and-whats-not-in-a-uri-query]]
//     */
//    case
//    class Plain(v: String, encoding: Encoding = QuerySegmentEncoding.StandardValue) extends QuerySegment
//    {
//      override fun toString = s"Plain($v,[encoding])"
//    }
//
//    private [model] fun fromQueryParams(mqp: QueryParams): Iterable[QuerySegment] =
//    {
//      mqp.toMultiSeq.flatMap {
//        case(k, vs) =>
//        vs match {
//          case Seq () => List(Value(k))
//          case s => s . com.fortysevendegrees.thool.map (v => KeyValue(k, v))
//        }
//      }
//    }

data class Authority(val userInfo: UserInfo?, val hostSegment: Segment, val port: Int?) {
//
//    /** Replace the user info with a username only. */
//    fun userInfo (username: String): Authority = this.copy(userInfo = Some(UserInfo(username, None)))
//
//    /** Replace the user info with username/password combination. */
//    fun userInfo (username: String, password: String): Authority =
//    this.copy(userInfo = Some(UserInfo(username, Some(password))))
//
//    /** Replace the user info. */
//    fun userInfo (ui: Option[UserInfo]): Authority = this.copy(userInfo = ui)
//
//    /** Replace the host. Does not validate the new host value if it's nonempty. */
//    fun host (h: String): Authority = hostSegment(HostSegment(h))
//
//    /** Replace the host. Does not validate the new host value if it's nonempty. */
//    fun hostSegment (s: Segment): Authority = this.copy(hostSegment = s)
//
//    fun host : String = hostSegment . v
//
//      /** Replace the port. */
//      fun port (p: Int): Authority = port(Some(p))
//
//    /** Replace the port. */
//    fun port (p: Option[Int]): Authority = this.copy(port = p)
//
//    override fun toString: String = {
//    fun encodeUserInfo (ui: UserInfo): String =
//    encode(Rfc3986.UserInfo)(ui.username) + ui.password.fold("")(":" + encode(Rfc3986.UserInfo)(_))
//
//    val userInfoS = userInfo.fold("")(encodeUserInfo(_) + "@")
//    val hostS = hostSegment.encoded
//    val portS = port.fold("")(":" + _)
//
//    s"//$userInfoS$hostS$portS"
//  }
}
//  object Authority {
//    private [model]
//    val Empty = Authority("")
//
//    fun safeApply(host: String): Either[String, Authority] =
//    Validate.all(validateHost(Some(host)))(Authority(None, HostSegment(host), None))
//    fun safeApply(host: String, port: Int): Either[String, Authority] =
//    Validate.all(validateHost(Some(host)))(Authority(None, HostSegment(host), Some(port)))
//    fun unsafeApply(host: String): Authority = safeApply(host).getOrThrow
//    fun unsafeApply(host: String, port: Int): Authority = safeApply(host, port).getOrThrow
//    fun apply(host: String): Authority = Authority(None, HostSegment(host), None)
//    fun apply(host: String, port: Int): Authority = Authority(None, HostSegment(host), Some(port))
//  }
