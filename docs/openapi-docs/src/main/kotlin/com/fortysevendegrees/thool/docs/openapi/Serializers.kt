package com.fortysevendegrees.thool.docs.openapi

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import com.fortysevendeg.thool.model.StatusCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
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
    return NonEmptyList.fromList(list)
      .getOrElse { throw SerializationException("Found empty list but expected NonEmptyList") }
  }
}

internal class ReferencedSerializer<T>(
  private val dataSerializer: KSerializer<T>
) : KSerializer<Referenced<T>> {

  private val refDescriptor = buildClassSerialDescriptor("Reference") {
    element<String>(RefKey)
  }

  @InternalSerializationApi
  // TODO review SerialDescriptor. Should it describe the actual type or the com.fortysevendegrees.thool.docs.openapi.json model
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("com.fortysevendegrees.thool.docs.openapi.Referenced") {
      element("Ref", refDescriptor, isOptional = true)
      element("description", dataSerializer.descriptor, isOptional = true)
    }

  @InternalSerializationApi
  override fun serialize(encoder: Encoder, value: Referenced<T>) {
    when (value) {
      is Referenced.Other -> encoder.encodeSerializableValue(dataSerializer, value.value)
      is Referenced.Ref -> {
        val encoder = encoder.beginStructure(descriptor)
        encoder.encodeStringElement(refDescriptor, 0, value.value.ref)
        encoder.endStructure(descriptor)
      }
    }
  }

  @InternalSerializationApi
  override fun deserialize(decoder: Decoder): Referenced<T> =
    TODO(
      """
      Impossible atm since we cannot detect (peek) if it's a reference or not.
      However, we don't support parsing OpenAPI into Endpoint atm.
      """.trimIndent()
    )
}

internal class ExampleValueSerializer : KSerializer<ExampleValue> {

  @InternalSerializationApi
  override val descriptor: SerialDescriptor =
    buildSerialDescriptor(
      "com.fortysevendegrees.thool.docs.openapi.ExampleValueSerializer",
      SerialKind.CONTEXTUAL
    ) {}

  override fun serialize(encoder: Encoder, value: ExampleValue) {
    when (value) {
      is ExampleValue.Single -> encoder.encodeString(value.value)
      is ExampleValue.Multiple ->
        encoder.encodeSerializableValue(ListSerializer(String.serializer()), value.values)
    }
  }

  override fun deserialize(decoder: Decoder): ExampleValue =
    TODO(
      """
      Impossible atm since we cannot detect (peek) if it's a reference or not.
      However, we don't support parsing OpenAPI into Endpoint atm.
      """.trimIndent()
    )
}

internal class ResponsesSerializer : KSerializer<Responses> {
  override val descriptor: SerialDescriptor = ResponsesDescriptor
  private val elementSerializer = Referenced.serializer(Response.serializer())

  override fun deserialize(decoder: Decoder): Responses {
    val decoder = decoder.beginStructure(descriptor)
    val size = decoder.decodeCollectionSize(descriptor)
    var default: Referenced<Response>? = null
    val responses = LinkedHashMap<StatusCode, Referenced<Response>>(size)
    while (true) {
      val index = decoder.decodeElementIndex(descriptor)
      if (index == CompositeDecoder.DECODE_DONE) break
      val key: String = decoder.decodeSerializableElement(descriptor, index, String.serializer())
      if (key == "default") {
        default = decoder.decodeSerializableElement(descriptor, index + 1, elementSerializer)
      } else {
        val code = StatusCode(key.toInt())
        responses[code] = decoder.decodeSerializableElement(descriptor, index + 1, elementSerializer)
      }
    }

    return Responses(default, responses)
  }

  override fun serialize(encoder: Encoder, value: Responses) {
    val size = value.responses.size + (value.default?.let { 1 } ?: 0)
    val composite = encoder.beginCollection(descriptor, size)
    var index = 0
    value.default?.let {
      composite.encodeStringElement(descriptor, index++, "default")
      composite.encodeSerializableElement(descriptor, index++, elementSerializer, it)
    }
    value.responses.forEach { (k, v) ->
      composite.encodeSerializableElement(descriptor, index++, StatusCodeAsIntSerializer, k)
      composite.encodeSerializableElement(descriptor, index++, elementSerializer, v)
    }
    composite.endStructure(descriptor)
  }
}

@ExperimentalSerializationApi
object ResponsesDescriptor : SerialDescriptor {
  override val serialName: String = "com.fortysevendegrees.thool.docs.openapi.Responses"
  override val kind: SerialKind = StructureKind.MAP
  override val elementsCount: Int = 2
  private val valueDescriptor: SerialDescriptor =
    Referenced.serializer(Response.serializer()).descriptor
  private val keyDescriptor: SerialDescriptor =
    StatusCodeAsIntSerializer.descriptor

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
    return when (index % 2) {
      0 -> StatusCodeAsIntSerializer.descriptor
      1 -> valueDescriptor
      else -> error("Unreached")
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ResponsesDescriptor) return false
    if (serialName != serialName) return false
    if (keyDescriptor != keyDescriptor) return false
    if (valueDescriptor != valueDescriptor) return false
    return true
  }

  override fun hashCode(): Int {
    var result = serialName.hashCode()
    result = 31 * result + keyDescriptor.hashCode()
    result = 31 * result + valueDescriptor.hashCode()
    return result
  }

  override fun toString(): String = "$serialName($keyDescriptor, $valueDescriptor)"
}
