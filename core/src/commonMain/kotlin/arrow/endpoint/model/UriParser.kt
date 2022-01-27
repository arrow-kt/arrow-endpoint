package arrow.endpoint.model

import arrow.core.Either
import arrow.core.computations.RestrictedEitherEffect
import arrow.core.computations.either
import arrow.core.left
import arrow.endpoint.model.Rfc3986.decode

private val schemePattern = Regex("^([a-zA-Z][a-zA-Z0-9+\\-.]*):")
private val uriPartsRegex = Regex("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?")

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
      uriPartsRegex.matchEntire(trimmedUrl) ?: UriError.CantParse("Can't parse $trimmedUrl").left().bind()

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

/*private suspend fun RestrictedEitherEffect<UriError, *>.check(groupValues: List<String>): Unit {
  val validScheme =
    groupValues.getOrNull(2) ?: UriError.CantParse("Invalid Uri from String. Scheme is missing.").left().bind()

  val authority = groupValues.getOrNull(4)
  val path = groupValues.getOrNull(5) ?: ""
  val query = groupValues.getOrNull(7)
  val fragment = groupValues.getOrNull(9)

  val userInfoEndIndex = authority?.indexOf(USER_INFO_END_DELIMITER) ?: -1

  val userInfo = if (userInfoEndIndex == -1) null else authority?.substring(startIndex = 0, endIndex = userInfoEndIndex)
  val uI = getUserInfoOrNull(groupValues)
  val host = authority?.let {
    val hostStartIndex = if (userInfoEndIndex == -1) 0 else userInfoEndIndex + 1
    var hostIpv6EndIndex = it.indexOf(HOST_IPV6_END_DELIMITER)
    var portStartIndex = it.indexOf(PORT_START_DELIMITER)

    if (hostIpv6EndIndex != -1) {
      hostIpv6EndIndex += 1
    }

    if (hostIpv6EndIndex > portStartIndex) {
      portStartIndex = -1
    }

    val hostEndIndex =
      listOf(hostIpv6EndIndex, portStartIndex, it.length).filter { index -> index != -1 }.minOf { index -> index }

    it.substring(startIndex = hostStartIndex, endIndex = hostEndIndex)
  }

  val port = authority?.let {
    val hostIpv6EndIndex = it.indexOf(HOST_IPV6_END_DELIMITER)
    val portStartIndex = it.indexOf(PORT_START_DELIMITER)

    if (portStartIndex == -1 || hostIpv6EndIndex > portStartIndex) {
      null
    } else {
      it.substring(startIndex = portStartIndex + 1)
    }
  }?.toIntOrNull()

  println(validScheme)
  println(userInfo)
  println(host)
  println(port)
  println(path)
  println(query)
  println(fragment)
}*/

private suspend fun RestrictedEitherEffect<UriError, *>.getUserInfoOrNull(groupValues: List<String>): UserInfo? {
  val authority = groupValues.getOrNull(4)
  val userInfoEndIndex = authority?.indexOf(USER_INFO_END_DELIMITER) ?: -1
  val userInfo = if (userInfoEndIndex == -1) null else authority?.substring(startIndex = 0, endIndex = userInfoEndIndex)
  return userInfo?.let {
    val userName = userInfo.substringBefore(':').decode().bind()
    val pw = if (userInfo.indexOf(':') != -1) userInfo.substringAfter(':').decode().bind() else null
    UserInfo(userName, pw)
  }
}

private suspend fun RestrictedEitherEffect<UriError, *>.getHost(groupValues: List<String>): HostSegment {
  val authority = groupValues.getOrNull(4)
  val userInfoEndIndex = authority?.indexOf(USER_INFO_END_DELIMITER) ?: -1
  val host = authority?.let {
    val hostStartIndex = if (userInfoEndIndex == -1) 0 else userInfoEndIndex + 1
    var hostIpv6EndIndex = it.indexOf(HOST_IPV6_END_DELIMITER)
    var portStartIndex = it.indexOf(PORT_START_DELIMITER)

    if (hostIpv6EndIndex != -1) {
      hostIpv6EndIndex += 1
    }

    if (hostIpv6EndIndex > portStartIndex) {
      portStartIndex = -1
    }

    val hostEndIndex =
      listOf(hostIpv6EndIndex, portStartIndex, it.length).filter { index -> index != -1 }.minOrNull()!!

    it.substring(startIndex = hostStartIndex, endIndex = hostEndIndex)
  } ?: UriError.InvalidHost.left().bind()

  return host.removeSurrounding(prefix = "[", suffix = "]").let { host: String ->
    if (host.isNotEmpty() && host != " " && host != "\n" && host != "%20") HostSegment(
      v = host.decode().bind()
    )
    else UriError.InvalidHost.left().bind()
  }
}

private suspend fun RestrictedEitherEffect<UriError, *>.getPort(groupValues: List<String>, scheme: String): Int? {
  val authority = groupValues.getOrNull(4)
  val port: Int? = authority?.let {
    val hostIpv6EndIndex = it.indexOf(HOST_IPV6_END_DELIMITER)
    val portStartIndex = it.indexOf(PORT_START_DELIMITER)

    if (portStartIndex == -1 || hostIpv6EndIndex > portStartIndex) {
      null
    } else {
      it.substring(startIndex = portStartIndex + 1)
    }
  }?.toIntOrNull()

  return when {
    port == null || port.isDefaultPort(scheme) -> null // we can omit it
    port in 1..65535 -> port
    else -> UriError.InvalidPort.left().bind()
  }
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
