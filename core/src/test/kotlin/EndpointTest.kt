import arrow.core.right
import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.and
import com.fortysevendegrees.thool.input
import com.fortysevendegrees.thool.ktor.server.install
import com.fortysevendegrees.thool.output
import com.fortysevendegrees.thool.server.ServerEndpoint
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request

class EndpointTest : StringSpec({
  "endpoint should compose correctly" {
    Thool {
      val e1: Endpoint<String, Nothing, Unit> = Endpoint.input(query("q1", Codec.string))

      val e2: Endpoint<Pair<String, Int>, Nothing, Unit> =
        Endpoint.input(query("q1", Codec.string).and(query("q2", Codec.int)))

      val e3: Endpoint<String, Nothing, Unit> = Endpoint.input(header("h1", Codec.listHead(Codec.string)))

      val e4: Endpoint<Pair<String, Int>, Nothing, Unit> =
        Endpoint.input(header("h1", Codec.listHead(Codec.string)).and(header("h2", Codec.listHead(Codec.int))))

      val e5: Endpoint<Unit, Nothing, Unit> = Endpoint.input(fixedPath("p").and(fixedPath("p2")).and(fixedPath("p3")))

      val e6: Endpoint<String, Nothing, Unit> =
        Endpoint.input(fixedPath("p").and(fixedPath("p2")).and(fixedPath("p3")).and(path(Codec.string)))

      val e7: Endpoint<String, Nothing, Unit> =
        Endpoint.input(fixedPath("p").and(fixedPath("p2").and(fixedPath("p3")).and(path(Codec.string))))

      val e8: Endpoint<Pair<String, Int>, Nothing, Unit> = Endpoint.input(
        fixedPath("p").and(fixedPath("p2")).and(fixedPath("p3")).and(path(Codec.string)).and(path(Codec.int))
      )

      val e9: Endpoint<String, Nothing, Unit> = Endpoint.input(stringBody())

      val e10: Endpoint<Pair<String, Int>, Nothing, Unit> = Endpoint.input(stringBody()).input(path(Codec.int))
    }
  }
})
