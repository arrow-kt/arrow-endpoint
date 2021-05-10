import com.fortysevendegrees.thool.model.StatusCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.subclass

fun main() {

  val json = Json {
    prettyPrint = true
  }

  val api = OpenApi(
    Info(
      "Simple API overview",

      version = "1.2.3"
    ),
    paths = linkedMapOf(
      "/" to PathItem(
        get = Operation(
          operationId = "listVersionsv2",
          summary = "List API versions",
          responses = Responses(
            responses = linkedMapOf(
              StatusCode.Ok to Referenced.Other(
                Response(
                  description = "200 response",
                  content = linkedMapOf(
                    "application/json" to MediaType(
                      examples = linkedMapOf(
                        "foo" to Referenced.Other(
                          Example(
                            value = ExampleValue.Single("")
                          )
                        )
                      )
                    )
                  ),
                )
              )
            )
          )
        ),
        servers = emptyList(),
        parameters = emptyList()
      ),
    ),
    servers = emptyList(),
    components = Components(),
    security = emptyList(),
    tags = LinkedHashSet()
  )

  val string = json.encodeToString(api)
  println(string)

  val response: Referenced<Response> = Referenced.Other(
    Response(
      description = "200 response",
      content = linkedMapOf(
        "application/json" to MediaType(
          examples = linkedMapOf(
            "foo" to Referenced.Other(
              Example(
                value = ExampleValue.Single("")
              )
            )
          )
        )
      ),
    )
  )
  val responseString = json.encodeToString(response)
  println("Referenced.Other = $responseString")

  val ref: Referenced<Nothing> = Referenced.Ref(Reference("#/components/schemas/user"))
  val refString = json.encodeToString(ref)
  println("Referenced.Ref = $refString")
}
