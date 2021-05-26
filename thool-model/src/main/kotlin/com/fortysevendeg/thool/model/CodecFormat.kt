package com.fortysevendeg.thool.model

public sealed interface CodecFormat {
  public val mediaType: MediaType

  public object Json : CodecFormat {
    override val mediaType: MediaType = MediaType.ApplicationJson
  }

  public object Xml : CodecFormat {
    override val mediaType: MediaType = MediaType.ApplicationXml
  }

  public object TextPlain : CodecFormat {
    override val mediaType: MediaType = MediaType.TextPlain
  }

  public object TextHtml : CodecFormat {
    override val mediaType: MediaType = MediaType.TextHtml
  }

  public object OctetStream : CodecFormat {
    override val mediaType: MediaType = MediaType.ApplicationOctetStream
  }

  public object XWwwFormUrlencoded : CodecFormat {
    override val mediaType: MediaType = MediaType.ApplicationXWwwFormUrlencoded
  }

  public object MultipartFormData : CodecFormat {
    override val mediaType: MediaType = MediaType.MultipartFormData
  }

  public object Zip : CodecFormat {
    override val mediaType: MediaType = MediaType.ApplicationZip
  }

  public object TextEventStream : CodecFormat {
    override val mediaType: MediaType = MediaType.TextEventStream
  }
}
