package com.fortysevendegrees.tapir.model

sealed class CodecFormat {
  abstract val mediaType: MediaType

  object Json : CodecFormat() {
    override val mediaType = MediaType.ApplicationJson
  }

  object Xml : CodecFormat() {
    override val mediaType = MediaType.ApplicationXml
  }

  object TextPlain : CodecFormat() {
    override val mediaType = MediaType.TextPlain
  }

  object TextHtml : CodecFormat() {
    override val mediaType = MediaType.TextHtml
  }

  object OctetStream : CodecFormat() {
    override val mediaType = MediaType.ApplicationOctetStream
  }

  object XWwwFormUrlencoded : CodecFormat() {
    override val mediaType = MediaType.ApplicationXWwwFormUrlencoded
  }

  object MultipartFormData : CodecFormat() {
    override val mediaType = MediaType.MultipartFormData
  }

  object Zip : CodecFormat() {
    override val mediaType = MediaType.ApplicationZip
  }

  object TextEventStream : CodecFormat() {
    override val mediaType = MediaType.TextEventStream
  }

}