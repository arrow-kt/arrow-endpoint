package com.fortysevendegrees.tapir.server.intrepreter

import com.fortysevendegrees.tapir.RawBodyType
import kotlinx.coroutines.flow.Flow

interface RequestBody {
  //  val streams: Streams[S]
  suspend fun <R> toRaw(bodyType: RawBodyType<R>): R
  fun toFlow(): Flow<Byte>
//  def toStream(): streams.BinaryStream
}