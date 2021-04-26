package com.fortysevendegrees.thool.test

import com.fortysevendegrees.thool.Codec
import com.fortysevendegrees.thool.Endpoint
import com.fortysevendegrees.thool.EndpointInput
import com.fortysevendegrees.thool.Thool
import com.fortysevendegrees.thool.Thool.fixedPath
import com.fortysevendegrees.thool.Thool.header
import com.fortysevendegrees.thool.Thool.path
import com.fortysevendegrees.thool.Thool.query
import com.fortysevendegrees.thool.Thool.stringBody
import com.fortysevendegrees.thool.and
import com.fortysevendegrees.thool.input
import com.fortysevendegrees.thool.output

object TestEndpoint {

  data class Fruit(val name: String)
  data class FruitAmount(val fruit: String, val amount: Int)

  public val fruitParam: EndpointInput.Query<String> = query("fruit", Codec.string)

  public val in_query_out_string: Endpoint<String, Nothing, String> =
    Endpoint.get().input(fruitParam).output(stringBody())

  public val in_query_out_infallible_string: Endpoint<String, Nothing, String> =
    Endpoint.input(fruitParam).output(stringBody()).name("infallible")

  public val in_query_query_out_string: Endpoint<Pair<String, Int?>, Nothing, String> =
    Endpoint
      .get()
      .input(query("fruit", Codec.listFirst(Codec.string)).and(query("amount", Codec.listFirstOrNull(Codec.int))))
      .output(stringBody())

  public val in_header_out_string: Endpoint<String, Nothing, String> =
    Endpoint.input(Thool.header("X-Role", Codec.listFirst(Codec.string))).output(stringBody())

  public val in_path_path_out_string: Endpoint<Pair<String, Int>, Nothing, String> =
    Endpoint.get { "fruit" / path(Codec.string) / "amount" / path(Codec.int) }.output(stringBody())

  public val in_two_path_capture: Endpoint<Pair<Int, Int>, Nothing, Pair<Int, Int>> =
    Endpoint
      .get { "in" / path(Codec.int) / path(Codec.int) }
      .output(header("a", Codec.listFirst(Codec.int)).and(header("b", Codec.listFirst(Codec.int))))

  public val in_string_out_string: Endpoint<String, Nothing, String> =
    Endpoint
      .post { "api" / "echo" }
      .input(stringBody())
      .output(stringBody())

  public val in_path: Endpoint<String, Nothing, Unit> =
    Endpoint
      .get("api")
      .input(path(Codec.string))

//  public val in_fixed_header_out_string: Endpoint<Unit, Nothing, String> =
//    Endpoint
//      .get("secret")
//      .input(header("location", "secret"))
//      .output(stringBody())

  public val in_mapped_query_out_string: Endpoint<List<Char>, Nothing, String> =
    Endpoint
      .get()
      .input(fruitParam.map(String::toList) { it.joinToString(separator = "") })
      .output(stringBody())
      .name("mapped query")

  public val in_mapped_path_out_string: Endpoint<Fruit, Nothing, String> =
    Endpoint
      .get()
      .input(fixedPath("fruit").and(path(Codec.string)).map(::Fruit, Fruit::name))
      .output(stringBody())
      .name("mapped path")

  public val in_mapped_path_path_out_string: Endpoint<FruitAmount, Nothing, String> =
    Endpoint
      .get()
      .input(
        fixedPath("fruit").and(path(Codec.string)).and(fixedPath("amount")).and(path(Codec.int))
          .map({ (name, amount) -> FruitAmount(name, amount) }, { Pair(it.fruit, it.amount) })
      ).output(stringBody())
      .name("mapped path path")

//  val in_query_mapped_path_path_out_string: Endpoint[(FruitAmount, String), Unit, String, Any] = endpoint
//  .in (("fruit" / path[String] / "amount" / path[Int]).mapTo(FruitAmount))
//  .in (query[String]("color"))
//  .out (stringBody)
//  .name("query and mapped path path")
//
//  val in_query_out_mapped_string: Endpoint[String, Unit, List[Char], Any] =
//  endpoint.in (query[String]("fruit")).out (stringBody.map(_.toList)(_.mkString(""))).name("out mapped")
//
//  val in_query_out_mapped_string_header: Endpoint[String, Unit, FruitAmount, Any] = endpoint
//  .in (query[String]("fruit"))
//  .out (stringBody.and(header [Int]("X-Role")).mapTo(FruitAmount))
//  .name("out mapped")
//
//  val in_header_before_path: Endpoint[(String, Int), Unit, (Int, String), Any] = endpoint
//  .in (header [String]("SomeHeader"))
//  .in (path[Int])
//  .out (header [Int]("IntHeader") and stringBody)
//
//  val in_json_out_json: Endpoint[FruitAmount, Unit, FruitAmount, Any] =
//  endpoint.post.in ("api" / "echo")
//  .in (jsonBody[FruitAmount])
//  .out (jsonBody[FruitAmount]).name("echo json")
//
//  val in_content_type_fixed_header: Endpoint[Unit, Unit, Unit, Any] =
//  endpoint.post.in ("api" / "echo")
//  .in (header (Header.contentType(MediaType.ApplicationJson)))
}
