package arrow.endpoint.server.interpreter

import arrow.endpoint.EndpointIO

public interface RequestBody {
  public suspend fun <R> toRaw(bodyType: EndpointIO.Body<R, *>): R
}
