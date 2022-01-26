package arrow.endpoint.model

import arrow.core.Either
import arrow.core.computations.either
import arrow.core.left
import arrow.core.right
import arrow.endpoint.model.Rfc3986.decode

private val schemePattern =
  Regex("^([a-zA-Z][a-zA-Z0-9+\\-.]*):")

@Suppress("RegExpRedundantEscape")
private val schemeSpecificPartPattern =
  Regex("^?(//(?<authority>((?<userinfo>[^/?#]*)@)?(?<host>(\\[[^\\]]*\\]|[^/?#:]*))(:(?<port>[^/?#]*))?))?(?<path>[^?#]*)(\\?(?<query>[^#]*))?(#(?<fragment>.*))?")

private val uriPartsRegex =
  Regex("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?")

@Suppress("FunctionName")
public fun Uri(url: String): Uri? =
  parseToUri(url).orNull()

public fun parseToUri(url: String): Either<UriError, Uri> =
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
