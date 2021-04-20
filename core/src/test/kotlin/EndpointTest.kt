import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.and
import com.fortysevendegrees.thool.input
import io.kotest.core.spec.style.StringSpec

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
