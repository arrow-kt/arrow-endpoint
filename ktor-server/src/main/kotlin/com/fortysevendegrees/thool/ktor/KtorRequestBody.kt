package com.fortysevendegrees.thool.ktor

import com.fortysevendegrees.thool.RawBodyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import com.fortysevendegrees.thool.server.intrepreter.RequestBody
import io.ktor.application.ApplicationCall
import io.ktor.util.cio.toByteArray
import io.ktor.utils.io.consumeEachBufferRange
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.nio.ByteBuffer

internal class KtorRequestBody(val ctx: ApplicationCall) : RequestBody {
  override suspend fun <R> toRaw(bodyType: RawBodyType<R>): R {
    val body = ctx.request.receiveChannel()
    return when (bodyType) {
      RawBodyType.ByteArrayBody -> body.toByteArray()
      RawBodyType.ByteBufferBody -> ByteBuffer.wrap(body.toByteArray())
      RawBodyType.InputStreamBody -> body.toInputStream()
      is RawBodyType.StringBody -> body.toByteArray().toString(bodyType.charset)
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
