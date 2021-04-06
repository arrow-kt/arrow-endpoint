package com.fortysevendegrees.tapir.ktor

import com.fortysevendegrees.tapir.ConnectionInfo
import com.fortysevendegrees.tapir.ServerRequest
import com.fortysevendegrees.thool.model.Header
import com.fortysevendegrees.thool.model.Method
import com.fortysevendegrees.thool.model.QueryParams
import com.fortysevendegrees.thool.model.Uri
import io.ktor.application.ApplicationCall
import io.ktor.request.httpMethod
import io.ktor.request.httpVersion
import io.ktor.request.path
import io.ktor.util.flattenEntries

internal class KtorServerRequest(val ctx: ApplicationCall) : ServerRequest {
  override val protocol: String = ctx.request.httpVersion
  override val connectionInfo: ConnectionInfo by lazy { ConnectionInfo(null, null, null) }
  override val underlying: Any = ctx

  override val uri: Uri
    get() = TODO("Uri.unsafeParse(ctx.request.uri.toString())")

  // TODO fix with proper path decoding
  override fun pathSegments(): List<String> =
    ctx.request.path().dropWhile { it == '/' }.split("/")

  override fun queryParameters(): QueryParams =
    QueryParams(ctx.request.queryParameters.entries().map(Map.Entry<String, List<String>>::toPair))

  override val method: Method =
    Method(ctx.request.httpMethod.value)

  override val headers: List<Header> =
    ctx.request.headers.flattenEntries().map { (name, value) -> Header(name, value) }
}