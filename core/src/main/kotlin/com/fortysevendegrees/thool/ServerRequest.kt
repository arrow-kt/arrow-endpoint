package com.fortysevendegrees.thool

import com.fortysevendegrees.thool.model.QueryParams
import com.fortysevendegrees.thool.model.RequestMetadata

public interface ServerRequest : RequestMetadata {
  val protocol: String
  val connectionInfo: ConnectionInfo
  val underlying: Any

  /** Can differ from `uri.path`, if the endpoint is deployed in a context */
  fun pathSegments(): List<String>
  fun queryParameters(): QueryParams
}

public data class Address(val hostname: String, val port: Int)
public data class ConnectionInfo(val local: Address?, val remote: Address?, val secure: Boolean?)
