import arrow.core.Either
import com.fortysevendegrees.thool.DecodeResult
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.ktor.client.requestAndParse
import com.fortysevendegrees.thool.output
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

class CioTests : StringSpec({
  "test 47deg site" {
    val client =
      HttpClient(CIO)

    val getBlog =
      Endpoint
        .get { "" / "blog" }
        .output(Thool.stringBody())

    val f = getBlog.requestAndParse("https://www.47deg.com")

    val response = client.f(Unit).shouldBeInstanceOf<DecodeResult.Value<Either.Right<String>>>()

    println(response.value.value)

    client.close()
  }
})
