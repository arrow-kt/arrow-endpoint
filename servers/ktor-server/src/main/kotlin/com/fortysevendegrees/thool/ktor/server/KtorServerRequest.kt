package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.model.Address
import com.fortysevendegrees.thool.model.Authority
import com.fortysevendegrees.thool.model.ConnectionInfo
import com.fortysevendegrees.thool.model.Header
import com.fortysevendegrees.thool.model.HostSegment
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.PathSegments
import com.fortysevendegrees.thool.model.QuerySegment
import com.fortysevendegrees.thool.model.ServerRequest
import com.fortysevendegrees.thool.model.Uri
import io.ktor.application.ApplicationCall
import io.ktor.features.origin
import io.ktor.http.RequestConnectionPoint
import io.ktor.request.host
import io.ktor.request.httpMethod
import io.ktor.request.httpVersion
import io.ktor.request.path
import io.ktor.request.port
import io.ktor.util.flattenEntries

public fun ApplicationCall.toServerRequest(): ServerRequest {
  val uri = Uri(
    request.origin.scheme,
    Authority(null, HostSegment(request.host()), request.port()),
    PathSegments.absoluteOrEmptyS(request.path().removePrefix("/").split("/")),
    request.queryParameters.entries().flatMap { (name, values) ->
      values.map { QuerySegment.KeyValue(name, it) }
    },
    null
  )
  return ServerRequest(
    protocol = request.httpVersion,
    connectionInfo = ConnectionInfo(request.origin.toAddress(), null, null),
    method = Method(request.httpMethod.value),
    uri = uri,
    headers = request.headers.flattenEntries().map { (name, value) -> Header(name, value) },
    pathSegments = uri.path(),
    queryParameters = uri.params()
  )
}

private fun RequestConnectionPoint.toAddress(): Address = Address(host, port)
