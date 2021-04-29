package com.fortysevendegrees.thool.server.interpreter

import com.fortysevendegrees.thool.EndpointIO

public interface RequestBody {
  public suspend fun <R> toRaw(bodyType: EndpointIO.Body<R, *>): R
}
