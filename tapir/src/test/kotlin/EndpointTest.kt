import com.fortysevendegrees.tapir.Codec
import com.fortysevendegrees.tapir.Endpoint
import com.fortysevendegrees.tapir.Tapir
import com.fortysevendegrees.tapir.and
import com.fortysevendegrees.tapir.withInput
import io.kotest.core.spec.style.StringSpec

class EndpointTest : StringSpec({
  "endpoint should compose correctly" {
    Tapir {
      val e1: Endpoint<String, Unit, Unit> = endpoint.withInput(query("q1", Codec.string))

      val e2: Endpoint<Pair<String, Int>, Unit, Unit> =
        endpoint.withInput(query("q1", Codec.string).and(query("q2", Codec.int)))

      val e3: Endpoint<String, Unit, Unit> = endpoint.withInput(header("h1", Codec.listHead(Codec.string)))

      val e4: Endpoint<Pair<String, Int>, Unit, Unit> =
        endpoint.withInput(header("h1", Codec.listHead(Codec.string)).and(header("h2", Codec.listHead(Codec.int))))

      val e5: Endpoint<Unit, Unit, Unit> = endpoint.withInput(fixedPath("p").and(fixedPath("p2")).and(fixedPath("p3")))

      val e6: Endpoint<String, Unit, Unit> =
        endpoint.withInput(fixedPath("p").and(fixedPath("p2")).and(fixedPath("p3")).and(path(Codec.string)))

      val e7: Endpoint<String, Unit, Unit> =
        endpoint.withInput(fixedPath("p").and(fixedPath("p2").and(fixedPath("p3")).and(path(Codec.string))))

      val e8: Endpoint<Pair<String, Int>, Unit, Unit> = endpoint.withInput(
        fixedPath("p").and(fixedPath("p2")).and(fixedPath("p3")).and(path(Codec.string)).and(path(Codec.int))
      )

      val e9: Endpoint<String, Unit, Unit> = endpoint.withInput(stringBody())

      val e10: Endpoint<Pair<String, Int>, Unit, Unit> = endpoint.withInput(stringBody()).withInput(path(Codec.int))
    }
  }
})
