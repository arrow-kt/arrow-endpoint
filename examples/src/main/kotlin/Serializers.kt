import arrow.core.NonEmptyList
import arrow.core.getOrElse
import com.fortysevendegrees.thool.model.StatusCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    return NonEmptyList.fromList(list)
      .getOrElse { throw SerializationException("Found empty list but expected NonEmptyList") }
  }
}

internal class ReferencedSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Referenced<T>> {

  @InternalSerializationApi
  override val descriptor: SerialDescriptor = buildSerialDescriptor("Referenced", PolymorphicKind.SEALED) {
    element("Ref", buildClassSerialDescriptor("Reference") {
      element<String>(RefKey)
    })
//    element("Ref", buildClassSerialDescriptor("Ref") {
//      element("value", buildClassSerialDescriptor("Reference") {
//        element<String>(RefKey)
//      })
//    })
//    element("Ref", Reference.serializer().descriptor)
//    element(RefKey, Reference.serializer().descriptor)
    element("Other", dataSerializer.descriptor)
  }

  override fun serialize(encoder: Encoder, value: Referenced<T>) {
    require(encoder is JsonEncoder)
    val jsonElement = when (value) {
      is Referenced.Ref -> buildJsonObject { put(RefKey, JsonPrimitive(value.value.ref)) }
      is Referenced.Other -> encoder.json.encodeToJsonElement(dataSerializer, value.value)
    }
    encoder.encodeJsonElement(jsonElement)
  }

  override fun deserialize(decoder: Decoder): Referenced<T> {
    require(decoder is JsonDecoder)
    val jsonElement = decoder.decodeJsonElement()
    if (jsonElement is JsonObject && RefKey in jsonElement)
      return Referenced.Ref(Reference(jsonElement[RefKey]!!.jsonPrimitive.content))
    return Referenced.Other(decoder.json.decodeFromJsonElement(dataSerializer, jsonElement))
  }
}
