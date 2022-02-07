package arrow.endpoint.model

import arrow.endpoint.model.Rfc3986.encode
import io.ktor.utils.io.charsets.Charsets

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
    /** Encodes all reserved characters. */
    public val All: Encoding
      get() = {
        it.encodeURLQueryComponent(encodeFull = true, charset = Charsets.UTF_8)
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
