import arrow.core.NonEmptyList
import arrow.core.getOrElse
import com.fortysevendegrees.thool.model.StatusCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

internal object StatusCodeAsIntSerializer : KSerializer<StatusCode> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StatusCode", PrimitiveKind.INT)
  override fun serialize(encoder: Encoder, value: StatusCode) = encoder.encodeInt(value.code)
  override fun deserialize(decoder: Decoder): StatusCode = StatusCode(decoder.decodeInt())
}

internal object BigDecimalAsStringSerializer : KSerializer<BigDecimal> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}

@ExperimentalSerializationApi
internal class NelDescriptor(val elementDescriptor: SerialDescriptor) : SerialDescriptor {
  override val serialName: String = "arrow.core.NonEmptyList"
  override val kind: SerialKind = StructureKind.LIST
  override val elementsCount: Int = 1

  override fun getElementName(index: Int): String = index.toString()
  override fun getElementIndex(name: String): Int =
    name.toIntOrNull() ?: throw IllegalArgumentException("$name is not a valid list index")

  override fun isElementOptional(index: Int): Boolean {
    require(index >= 0) { "Illegal index $index, $serialName expects only non-negative indices" }
    return false
  }

  override fun getElementAnnotations(index: Int): List<Annotation> {
    require(index >= 0) { "Illegal index $index, $serialName expects only non-negative indices" }
    return emptyList()
  }

  override fun getElementDescriptor(index: Int): SerialDescriptor {
    require(index >= 0) { "Illegal index $index, $serialName expects only non-negative indices" }
    return elementDescriptor
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is NelDescriptor) return false
    if (elementDescriptor == other.elementDescriptor && serialName == other.serialName) return true
    return false
  }

  override fun hashCode(): Int {
    return elementDescriptor.hashCode() * 31 + serialName.hashCode()
  }

  override fun toString(): String = "$serialName($elementDescriptor)"
}

internal class NelSerializer<T>(private val elementSerializer: KSerializer<T>) : KSerializer<NonEmptyList<T>> {
  override val descriptor: SerialDescriptor = NelDescriptor(elementSerializer.descriptor)

  override fun serialize(encoder: Encoder, value: NonEmptyList<T>) {
    val size = value.size
    val composite = encoder.beginCollection(descriptor, size)
    value.forEachIndexed { i, t -> composite.encodeSerializableElement(descriptor, i, elementSerializer, t) }
    composite.endStructure(descriptor)
  }

  override fun deserialize(decoder: Decoder): NonEmptyList<T> {
    val compositeDecoder = decoder.beginStructure(descriptor)
    val size = compositeDecoder.decodeCollectionSize(descriptor)
    val list = if (compositeDecoder.decodeSequentially()) {
      List(size) { index ->
        compositeDecoder.decodeSerializableElement(descriptor, index, elementSerializer)
      }
    } else {
      val builder = ArrayList<T>(size)
      while (true) {
        val index = compositeDecoder.decodeElementIndex(descriptor)
        if (index == CompositeDecoder.DECODE_DONE) break
        builder.add(index, compositeDecoder.decodeSerializableElement(descriptor, index, elementSerializer))
      }
      builder
    }
    compositeDecoder.endStructure(descriptor)
    return NonEmptyList.fromList(list).getOrElse { throw SerializationException("Found empty list but expected NonEmptyList") }
  }
}
