package com.fortysevendeg.thool.test

import com.fortysevendeg.thool.Codec
import com.fortysevendeg.thool.DecodeResult
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.EndpointInput
import com.fortysevendeg.thool.Schema
import com.fortysevendeg.thool.Thool.anyJsonBody
import com.fortysevendeg.thool.Thool.byteArrayBody
import com.fortysevendeg.thool.Thool.byteBufferBody
import com.fortysevendeg.thool.Thool.cookie
import com.fortysevendeg.thool.Thool.fixedPath
import com.fortysevendeg.thool.Thool.formBody
import com.fortysevendeg.thool.Thool.header
import com.fortysevendeg.thool.Thool.inputStreamBody
import com.fortysevendeg.thool.Thool.path
import com.fortysevendeg.thool.Thool.paths
import com.fortysevendeg.thool.Thool.query
import com.fortysevendeg.thool.Thool.queryParams
import com.fortysevendeg.thool.Thool.statusCode
import com.fortysevendeg.thool.Thool.stringBody
import com.fortysevendeg.thool.and
import com.fortysevendeg.thool.input
import com.fortysevendeg.thool.model.CodecFormat
import com.fortysevendeg.thool.model.MediaType
import com.fortysevendeg.thool.model.QueryParams
import com.fortysevendeg.thool.model.StatusCode
import com.fortysevendeg.thool.output
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.ByteBuffer

object TestEndpoint {

  public val fruitParam: EndpointInput.Query<String> = query("fruit", Codec.string)

  public val in_query_out_string: Endpoint<String, Unit, String> =
    Endpoint.get().input(fruitParam).output(stringBody())

  public val in_query_out_infallible_string: Endpoint<String, Unit, String> =
    Endpoint.input(fruitParam).output(stringBody()).name("infallible")

  public val in_query_query_out_string: Endpoint<Pair<String, Int?>, Unit, String> =
    Endpoint
      .get()
      .input(query("fruit", Codec.listFirst(Codec.string)).and(query("amount", Codec.listFirstOrNull(Codec.int))))
      .output(stringBody())

  public val in_header_out_string: Endpoint<String, Unit, String> =
    Endpoint.input(header("X-Role", Codec.listFirst(Codec.string))).output(stringBody())

  public val in_path_path_out_string: Endpoint<Pair<String, Int>, Unit, String> =
    Endpoint.get { "fruit" / path(Codec.string) / "amount" / path(Codec.int) }.output(stringBody())

  public val in_two_path_capture: Endpoint<Pair<Int, Int>, Unit, Pair<Int, Int>> =
    Endpoint
      .get { "in" / path(Codec.int) / path(Codec.int) }
      .output(header("a", Codec.listFirst(Codec.int)).and(header("b", Codec.listFirst(Codec.int))))

  public val in_string_out_string: Endpoint<String, Unit, String> =
    Endpoint
      .post { "api" / "echo" }
      .input(stringBody())
      .output(stringBody())

  public val in_path: Endpoint<String, Unit, Unit> =
    Endpoint
      .get("api")
      .input(path(Codec.string))

  public val in_mapped_query_out_string: Endpoint<List<Char>, Unit, String> =
    Endpoint
      .get()
      .input(fruitParam.map(String::toList) { it.joinToString(separator = "") })
      .output(stringBody())
      .name("mapped query")

  public val in_mapped_path_out_string: Endpoint<Fruit, Unit, String> =
    Endpoint
      .get()
      .input(fixedPath("fruit").and(path(Codec.string)).map(::Fruit, Fruit::name))
      .output(stringBody())
      .name("mapped path")

  public val in_mapped_path_path_out_string: Endpoint<FruitAmount, Unit, String> =
    Endpoint
      .get()
      .input(
        fixedPath("fruit").and(path(Codec.string)).and(fixedPath("amount")).and(path(Codec.int))
          .map({ (name, amount) -> FruitAmount(name, amount) }, { Pair(it.fruit, it.amount) })
      ).output(stringBody())
      .name("mapped path path")

  public val in_query_mapped_path_path_out_string: Endpoint<Pair<FruitAmount, String>, Unit, String> =
    Endpoint
      .get {
        ("fruit" / path(Codec.string) / path(Codec.int))
          .map({ (name, amount) -> FruitAmount(name, amount) }, { Pair(it.fruit, it.amount) })
      }
      .input(query("color", Codec.string))
      .output(stringBody())
      .name("query and mapped path path")

  public val in_query_out_mapped_string: Endpoint<String, Unit, List<Char>> =
    Endpoint
      .input(fruitParam)
      .output(stringBody().map({ it.toList() }, { it.joinToString("") }))
      .name("out mapped")

  public val in_query_out_mapped_string_header: Endpoint<String, Unit, FruitAmount> =
    Endpoint
      .input(fruitParam)
      .output(
        stringBody().and(header("X-Role", Codec.listFirst(Codec.int)))
          .map({ (name, amount) -> FruitAmount(name, amount) }, { Pair(it.fruit, it.amount) })
      )
      .name("out mapped")

  public val in_header_before_path: Endpoint<Pair<String, Int>, Unit, Pair<Int, String>> =
    Endpoint
      .input(header("SomeHeader", Codec.listFirst(Codec.string)))
      .input(path(Codec.int))
      .output(header("IntHeader", Codec.listFirst(Codec.int)).and(stringBody()))

  public val in_json_out_json: Endpoint<FruitAmount, Unit, FruitAmount> =
    Endpoint
      .post { "api" / "echo" }
      .input(anyJsonBody(Codec.jsonFruitAmount()))
      .output(anyJsonBody(Codec.jsonFruitAmount()))
      .name("echo json")

  public val Codec.Companion.mediaType: Codec<String, MediaType, CodecFormat.TextPlain>
    get() = string.mapDecode({ DecodeResult.Failure.Mismatch("", "") }) { it.toString() }

  public val in_content_type_header_with_custom_decode_results: Endpoint<MediaType, Unit, Unit> =
    Endpoint.post { "api" / "echo" }
      .input(header("Content-Type", Codec.listFirst(Codec.mediaType)))

  public val in_byte_array_out_byte_array: Endpoint<ByteArray, Unit, ByteArray> =
    Endpoint.post { "api" / "echo" }
      .input(byteArrayBody())
      .output(byteArrayBody())
      .name("echo byte array")

  public val in_byte_buffer_out_byte_buffer: Endpoint<ByteBuffer, Unit, ByteBuffer> =
    Endpoint.post { "api" / "echo" }
      .input(byteBufferBody())
      .output(byteBufferBody())
      .name("echo byte buffer")

  public val in_input_stream_out_input_stream: Endpoint<InputStream, Unit, InputStream> =
    Endpoint.post { "api" / "echo" }
      .input(inputStreamBody())
      .output(inputStreamBody())
      .name("echo input stream")

  public val in_string_out_stream_with_header: Endpoint<String, Unit, Pair<InputStream, Long?>> =
    Endpoint.post { "api" / "echo" }
      .input(stringBody())
      .output(inputStreamBody())
      .output(header("Content-Length", Codec.listFirstOrNull(Codec.long)))
      .name("input string output stream with header")

  public val in_unit_out_json_unit: Endpoint<Unit, Unit, Unit> =
    Endpoint.get { "api" / "unit" }
      .output(
        anyJsonBody(
          Codec.json(
            Schema.unit,
            { DecodeResult.Value(Json.decodeFromString(it)) },
            { Json.encodeToString(it) }
          )
        )
      )

  public val in_unit_out_string: Endpoint<Unit, Unit, String> =
    Endpoint.get("api")
      .output(stringBody())

  public val in_form_out_form: Endpoint<FruitAmount, Unit, FruitAmount> =
    Endpoint.post { "api" / "echo" }
      .input(formBody(Codec.formFruitAmount()))
      .output(formBody(Codec.formFruitAmount()))

  public val in_query_params_out_string: Endpoint<QueryParams, Unit, String> =
    Endpoint.get { "api" / "echo" / "params" }.input(queryParams()).output(stringBody())

  public val in_paths_out_string: Endpoint<List<String>, Unit, String> =
    Endpoint.get().input(paths()).output(stringBody())

  public val in_path_paths_out_header_body: Endpoint<Pair<Int, List<String>>, Unit, Pair<Int, String>> =
    Endpoint.get("api").input(path(Codec.int)).input(fixedPath("and")).input(paths())
      .output(header("IntPath", Codec.listFirst(Codec.int)).and(stringBody()))

  public val in_path_fixed_capture_fixed_capture: Endpoint<Pair<Int, Int>, Unit, Unit> =
    Endpoint.get { "customer" / path("customer_id", Codec.int) / "orders" / path("order_id", Codec.int) }

  public val in_query_list_out_header_list: Endpoint<List<String>, Unit, List<String>> =
    Endpoint.get { "api" / "echo" / "param-to-header" }
      .input(query("qq", Codec.list(Codec.string)))
      .output(header("hh", Codec.list(Codec.string)))

  public val in_cookie_cookie_out_header: Endpoint<Pair<Int, String>, Unit, List<String>> =
    Endpoint
      .get { "api" / "echo" / "headers" }
      .input(cookie("c1", Codec.nullableFirst(Codec.int)))
      .input(cookie("c2", Codec.nullableFirst(Codec.string)))
      .output(header("Set-Cookie", Codec.list(Codec.string)))

  public val in_root_path: Endpoint<Unit, Unit, Unit> = Endpoint.get("")

  public val in_single_path: Endpoint<Unit, Unit, Unit> = Endpoint.get("api")

  public val in_string_out_status: Endpoint<String, Unit, StatusCode> =
    Endpoint.input(fruitParam).output(statusCode())

  public val delete_endpoint: Endpoint<Unit, Unit, Unit> =
    Endpoint.delete { "api" / "delete" }
      .output(statusCode(StatusCode.Ok).description("ok"))

  public val in_optional_json_out_optional_json: Endpoint<FruitAmount?, Unit, FruitAmount?> =
    Endpoint.post { "api" / "echo" }
      .input(anyJsonBody(Codec.jsonNullableFruitAmount()))
      .output(anyJsonBody(Codec.jsonNullableFruitAmount()))

  /* Helper function to narrow Pair back to `I` */
  private fun <I, E, O> addInputAndOutput(e: Endpoint<I, E, O>): Endpoint<Triple<I, String, String>, E, Triple<O, String, String>> =
    e.input(query("x", Codec.string))
      .input(query("y", Codec.string))
      .output(header("X", Codec.listFirst(Codec.string)))
      .output(header("Y", Codec.listFirst(Codec.string)))

  public val in_4query_out_4header_extended: Endpoint<Triple<Pair<String, String>, String, String>, Unit, Triple<Pair<String, String>, String, String>> =
    addInputAndOutput(
      Endpoint
        .get { "api" / "echo" / "param-to-upper-header" }
        .input(query("a", Codec.string))
        .input(query("b", Codec.string))
        .output(header("A", Codec.listFirst(Codec.string)))
        .output(header("B", Codec.listFirst(Codec.string)))
    )
}
