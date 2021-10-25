package arrow.endpoint.docs.openapi

import io.kotest.core.spec.style.StringSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ResponseSerializerTest : StringSpec({

  val json = Json {
    encodeDefaults = false
    prettyPrint = true
  }

  "default response" {
    val response = Response("response")
    val str = json.encodeToString(response)
  }

  "response with MediaType" {
    val response = Response(
      "response",
      content = mapOf(
        "text/plain" to MediaType(
          schema = Referenced.Other(
            value = Schema()
          )
        )
      )
    )
    val str = json.encodeToString(response)
  }

  "Referenced.Other" {
    val schema = Referenced.Other(
      Schema()
    ) as Referenced<Schema>
    val str = json.encodeToString(schema)
  }

  "MediaType" {
    val schema = MediaType(
      schema = Referenced.Other(
        value = Schema()
      )
    )
    val str = json.encodeToString(schema)
  }
})
