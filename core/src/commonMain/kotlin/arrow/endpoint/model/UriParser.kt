package arrow.endpoint.model

import arrow.core.Either
import arrow.core.computations.RestrictedEitherEffect
import arrow.core.computations.either
import arrow.core.identity
import arrow.core.left
import arrow.endpoint.model.Rfc3986.decode

private val schemePattern = Regex("^([a-zA-Z][a-zA-Z0-9+\\-.]*):")
private val uriRegex = Regex("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?")

private const val PORT_START_DELIMITER = ':'
private const val USER_INFO_END_DELIMITER = '@'
private const val HOST_IPV6_END_DELIMITER = ']'

@Suppress("FunctionName")
public fun Uri(url: String): Uri? = parseToUri(url).orNull()

public fun parseToUri(url: String): Either<UriError, Uri> =
  either.eager {
    val trimmedUrl = url.trimStart()
    val scheme = schemePattern.find(trimmedUrl)?.value?.substringBefore(':')?.lowercase() ?: ""

    when (scheme) {
      "http", "https" -> Unit // continue
      else -> UriError.UnexpectedScheme("Unexpected scheme: $scheme").left().bind()
    }

    val match: MatchResult =
      uriRegex.matchEntire(trimmedUrl) ?: UriError.CantParse("Can't parse $trimmedUrl").left().bind()

    val groupValues = match.groupValues

    Uri(
      scheme = scheme.decode().bind(),
      authority = Authority(
        userInfo = getUserInfoOrNull(groupValues),
        hostSegment = getHost(groupValues),
        port = getPort(groupValues, scheme),
      ),
      pathSegments = getPathSegmentsOrEmpty(groupValues),
      querySegments = getQuerySegmentsOrEmpty(groupValues),
      fragmentSegment = getFragmentSegmentOrNull(groupValues)
    )
  }

private suspend fun RestrictedEitherEffect<UriError, *>.getUserInfoOrNull(groupValues: List<String>): UserInfo? =
  groupValues.getUserInfoFromAuthority?.let { userInfo ->
    val userName = userInfo.substringBefore(':').decode().bind()
    val pw = userInfo.takeIf { it.indexOf(':') != -1 }?.run { substringAfter(':').decode().bind() }
    UserInfo(userName, pw)
  }

private suspend fun RestrictedEitherEffect<UriError, *>.getHost(groupValues: List<String>): HostSegment {
  val host = groupValues.getHostAndPortFromAuthority?.let {
    val hostIpv6EndIndex =
      it.indexOf(HOST_IPV6_END_DELIMITER).let { index ->
        if (index != -1) index + 1 else index
      }

    val portStartIndex =
      it.lastIndexOf(PORT_START_DELIMITER).let { index ->
        if (hostIpv6EndIndex > index) -1 else index
      }

    val hostEndIndex =
      listOf(hostIpv6EndIndex, portStartIndex, it.length).filter { index -> index != -1 }.minOf(::identity)

    it.substring(0, endIndex = hostEndIndex)
  } ?: UriError.InvalidHost.left().bind()

  return host.removeSurrounding(prefix = "[", suffix = "]").let { host: String ->
    if (host.isNotEmpty() && host != " " && host != "\n" && host != "%20") HostSegment(
      v = host.decode().bind()
    )
    else UriError.InvalidHost.left().bind()
  }
}

private suspend fun RestrictedEitherEffect<UriError, *>.getPort(groupValues: List<String>, scheme: String): Int? {
  val port: Int? = groupValues.getHostAndPortFromAuthority?.let {
    val hostIpv6EndIndex = it.indexOf(HOST_IPV6_END_DELIMITER)
    val portStartIndex = it.lastIndexOf(PORT_START_DELIMITER)

    if (portStartIndex == -1 || hostIpv6EndIndex > portStartIndex) {
      null
    } else {
      val portString = it.substring(startIndex = portStartIndex + 1)
      if (portString.isNotEmpty()) {
        portString.toIntOrNull() ?: UriError.InvalidPort.left().bind()
      } else null // we can omit it
    }
  }

  return when {
    port == null || port.isDefaultPort(scheme) -> null // we can omit it
    port in 1..65535 -> port
    else -> UriError.InvalidPort.left().bind()
  }
}

private val List<String>.getUserInfoFromAuthority: String?
  get() = getOrNull(4)?.let { authority ->
    val userInfoEndIndex = authority.lastIndexOf(USER_INFO_END_DELIMITER)
    if (userInfoEndIndex == -1) null else authority.substring(startIndex = 0, endIndex = userInfoEndIndex)
  }

private val List<String>.getHostAndPortFromAuthority: String?
  get() = getOrNull(4)?.let { authority ->
    val userInfoEndIndex = authority.lastIndexOf(USER_INFO_END_DELIMITER)
    val hostStartIndex = if (userInfoEndIndex == -1) 0 else userInfoEndIndex + 1
    authority.substring(hostStartIndex).lowercase()
  }

private fun Int.isDefaultPort(scheme: String) = when (scheme) {
  "https" -> 443 == this
  else -> 80 == this
}

private suspend fun RestrictedEitherEffect<UriError, *>.getPathSegmentsOrEmpty(groupValues: List<String>): PathSegments =
  PathSegments.absoluteOrEmptyS(
    groupValues.getOrNull(5).orEmpty().let { pathPart ->
      when {
        pathPart.isEmpty() -> emptyList()
        else -> pathPart.removePrefix("/").split("/").map { segment -> segment.decode().bind() }
      }
    }
  )

private suspend fun RestrictedEitherEffect<UriError, *>.getQuerySegmentsOrEmpty(
  groupValues: List<String>
): List<QuerySegment> =
  groupValues.getOrNull(7)?.let { querySegments ->
    when (querySegments.contains("&") || querySegments.contains("=")) {
      true -> {
        querySegments.split("&").map { querySegment ->
          querySegment.split("=").map { it.decode(plusAsSpace = true).bind() }
        }.map { listQueryParams: List<String> ->
          when (listQueryParams.size) {
            1 -> QuerySegment.Value(listQueryParams.first())
            else -> QuerySegment.KeyValue(listQueryParams.first(),
              buildString { append(listQueryParams.drop(1).joinToString("=")) })
          }
        }
      }
      false -> {
        val query = querySegments.decode().bind().takeIf { it.isNotEmpty() }
        query?.let { listOf(QuerySegment.Plain(it)) } ?: emptyList()
      }
    }
  } ?: emptyList()

private suspend fun RestrictedEitherEffect<UriError, *>.getFragmentSegmentOrNull(groupValues: List<String>): FragmentSegment? =
  groupValues.getOrNull(9)?.let { fragment ->
    when (fragment.isNotEmpty()) {
      true -> FragmentSegment(v = fragment.decode().bind())
      false -> null
    }
  }
