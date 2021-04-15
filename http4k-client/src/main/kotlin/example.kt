import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.output
import org.http4k.client.ApacheClient

val pong: Endpoint<Unit, Nothing, String> =
  Endpoint
    .get("ping")
    .output(Thool.stringBody())

fun main() {
  val client = ApacheClient()
  val result = client(pong, "http://localhost:8080", Unit)
  println("$result")
}
