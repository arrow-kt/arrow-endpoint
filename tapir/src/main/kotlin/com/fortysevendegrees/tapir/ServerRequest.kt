package com.fortysevendegrees.tapir

import com.fortysevendegrees.tapir.model.QueryParams
import com.fortysevendegrees.tapir.model.RequestMetadata
import java.net.InetSocketAddress

interface ServerRequest : RequestMetadata {
  val protocol: String
  val connectionInfo: ConnectionInfo
  val underlying: Any

  /** Can differ from `uri.path`, if the endpoint is deployed in a context */
  fun pathSegments(): List<String>
  fun queryParameters(): QueryParams
}

data class ConnectionInfo(val local: InetSocketAddress?, val remote: InetSocketAddress?, val secure: Boolean?)