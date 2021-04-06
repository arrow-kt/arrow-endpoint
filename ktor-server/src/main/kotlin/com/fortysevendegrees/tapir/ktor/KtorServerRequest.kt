package com.fortysevendegrees.tapir.ktor

import com.fortysevendegrees.tapir.ConnectionInfo
import com.fortysevendegrees.tapir.ServerRequest
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.util.*
import com.fortysevendegrees.tapir.model.Header
import com.fortysevendegrees.tapir.model.Method
import com.fortysevendegrees.tapir.model.QueryParams
import com.fortysevendegrees.tapir.model.Uri

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