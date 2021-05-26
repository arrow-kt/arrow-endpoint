package com.fortysevendeg.thool.server.interpreter

import com.fortysevendeg.thool.EndpointIO

public interface RequestBody {
  public suspend fun <R> toRaw(bodyType: EndpointIO.Body<R, *>): R
}
