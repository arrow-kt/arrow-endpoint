package com.fortysevendegrees.thool.server.interpreter

import com.fortysevendegrees.thool.EndpointIO

public interface RequestBody {
  suspend fun <R> toRaw(bodyType: EndpointIO.Body<R, *>): R
}
