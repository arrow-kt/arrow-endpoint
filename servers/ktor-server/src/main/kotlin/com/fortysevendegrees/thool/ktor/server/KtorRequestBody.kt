package com.fortysevendegrees.thool.ktor.server

import com.fortysevendegrees.thool.EndpointIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import com.fortysevendegrees.thool.server.interpreter.RequestBody
import io.ktor.application.ApplicationCall
import io.ktor.util.toByteArray
import io.ktor.utils.io.consumeEachBufferRange
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.nio.ByteBuffer

internal class KtorRequestBody(val ctx: ApplicationCall) : RequestBody {
  override suspend fun <R> toRaw(bodyType: EndpointIO.Body<R, *>): R {
    val body = ctx.request.receiveChannel()
    return when (bodyType) {
      is EndpointIO.ByteArrayBody -> body.toByteArray()
      is EndpointIO.ByteBufferBody -> ByteBuffer.wrap(body.toByteArray())
      is EndpointIO.InputStreamBody -> body.toInputStream()
      is EndpointIO.StringBody -> body.toByteArray().toString(bodyType.charset)
    } as R
  }

  override fun toFlow(): Flow<Byte> = flow {
    ctx.request.receiveChannel().consumeEachBufferRange { buffer, last ->
      emit(buffer.toByteArray())
      !last
    }
  }.flatMapConcat { it.asFlow() }

  fun ByteBuffer.toByteArray(): ByteArray {
    val byteArray = ByteArray(capacity())
    get(byteArray)
    return byteArray
  }

  fun ByteArray.asFlow(): Flow<Byte> =
    flow { forEach { emit(it) } }
}
