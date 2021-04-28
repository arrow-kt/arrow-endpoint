package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.ConnectionInfo
import com.fortysevendegrees.thool.ServerRequest
import com.fortysevendegrees.thool.model.Authority
import com.fortysevendegrees.thool.model.Header
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.PathSegments
import com.fortysevendegrees.thool.model.QueryParams
import com.fortysevendegrees.thool.model.QuerySegment
import com.fortysevendegrees.thool.model.Uri
import io.ktor.application.ApplicationCall
import io.ktor.features.origin
import io.ktor.request.host
import io.ktor.request.httpMethod
import io.ktor.request.httpVersion
import io.ktor.request.path
import io.ktor.request.port
import io.ktor.util.flattenEntries

internal class KtorServerRequest(ctx: ApplicationCall) : ServerRequest {
  override val protocol: String = ctx.request.httpVersion
  override val connectionInfo: ConnectionInfo by lazy { ConnectionInfo(null, null, null) }
  override val underlying: Any = ctx

  override val uri: Uri = Uri(
    ctx.request.origin.scheme,
    Authority(null, ctx.request.host(), ctx.request.port()),
    PathSegments.absoluteOrEmptyS(ctx.request.path().removePrefix("/").split("/")),
    ctx.request.queryParameters.entries().flatMap { (name, values) ->
      values.map { QuerySegment.KeyValue(name, it) }
    },
    null
  )

  override fun pathSegments(): List<String> = uri.path()
  override fun queryParameters(): QueryParams = uri.params()
  override val method: Method = Method(ctx.request.httpMethod.value)
  override val headers: List<Header> =
    ctx.request.headers.flattenEntries().map { (name, value) -> Header(name, value) }
}
