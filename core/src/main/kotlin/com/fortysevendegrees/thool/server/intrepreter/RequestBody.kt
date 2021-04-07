package com.fortysevendegrees.thool.server.intrepreter

import com.fortysevendegrees.thool.RawBodyType
import kotlinx.coroutines.flow.Flow

interface RequestBody {
  //  val streams: Streams[S]
  suspend fun <R> toRaw(bodyType: RawBodyType<R>): R
  fun toFlow(): Flow<Byte>
//  def toStream(): streams.BinaryStream
}
