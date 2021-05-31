import com.fortysevendeg.thool.Codec
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.Thool
import com.fortysevendeg.thool.and
import com.fortysevendeg.thool.input
import io.kotest.core.spec.style.StringSpec

@Suppress("UNUSED_VARIABLE")
class EndpointTest : StringSpec({
  "endpoint should compose correctly" {
    Thool {
      val e1: Endpoint<String, Unit, Unit> = Endpoint.input(query("q1", Codec.string))

      val e2: Endpoint<Pair<String, Int>, Unit, Unit> =
        Endpoint.input(query("q1", Codec.string).and(query("q2", Codec.int)))

      val e3: Endpoint<String, Unit, Unit> = Endpoint.input(header("h1", Codec.listFirst(Codec.string)))

      val e4: Endpoint<Pair<String, Int>, Unit, Unit> =
        Endpoint.input(header("h1", Codec.listFirst(Codec.string)).and(header("h2", Codec.listFirst(Codec.int))))

      val e5: Endpoint<Unit, Unit, Unit> = Endpoint.input(fixedPath("p").and(fixedPath("p2")).and(fixedPath("p3")))

      val e6: Endpoint<String, Unit, Unit> =
        Endpoint.input(fixedPath("p").and(fixedPath("p2")).and(fixedPath("p3")).and(path(Codec.string)))

      val e7: Endpoint<String, Unit, Unit> =
        Endpoint.input(fixedPath("p").and(fixedPath("p2").and(fixedPath("p3")).and(path(Codec.string))))

      val e8: Endpoint<Pair<String, Int>, Unit, Unit> = Endpoint.input(
        fixedPath("p").and(fixedPath("p2")).and(fixedPath("p3")).and(path(Codec.string)).and(path(Codec.int))
      )

      val e9: Endpoint<String, Unit, Unit> = Endpoint.input(stringBody())

      val e10: Endpoint<Pair<String, Int>, Unit, Unit> = Endpoint.input(stringBody()).input(path(Codec.int))
    }
  }
})
