import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.output
import org.http4k.client.ApacheClient

val pong: Endpoint<Unit, Nothing, String> =
  Endpoint
    .get("ping")
    .output(Thool.stringBody())

fun main() {
  val (req, responseParser) = pong.toRequest("http://localhost:8080")(Unit)

  val client = ApacheClient()
  val x = client(req)
  val res = responseParser(x)
  println("http4k response: $x, result: $res")
}
