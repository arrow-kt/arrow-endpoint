package arrow.endpoint.model

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf

class UriTest : FunSpec() {

  private val v1 = "y"
  private val v2 = "a c"
  private val v2queryEncoded = "a+c"
  private val v2encoded = "a%20c"

  private val testData: List<Pair<String, List<Pair<String, String>>>> = listOf(
    "basic" to listOf(
      "http://example.com" to "http://example.com",
      "http://example.com/" to "http://example.com/",
      "http://example.com?x=y" to "http://example.com?x=y",
      "http://example.com/a/b/c" to "http://example.com/a/b/c",
      "http://example.com/a/b/c/" to "http://example.com/a/b/c/",
      "http://example.com/a/b/c?x=y&h=j" to "http://example.com/a/b/c?x=y&h=j"
    ),
    "scheme" to listOf(
      "https://example.com" to "https://example.com",
      "http://example.com:" to "http://example.com"
    ),
    "user info" to listOf(
      "http://user:pass@example.com" to "http://user:pass@example.com",
      "http://$v2@example.com" to "http://$v2encoded@example.com",
      "http://$v1:$v2@example.com" to "http://$v1:$v2encoded@example.com"
    ),
    "authority" to listOf(
      "http://$v1.com" to "http://$v1.com",
      "http://$v2.com" to "http://$v2encoded.com",
      "http://$v1.$v2.com" to "http://$v1.$v2encoded.com",
      "http://$v1$v2.com" to "http://$v1$v2encoded.com",
      "http://z$v1.com" to "http://z$v1.com",
      "http://sub.example.com" to "http://sub.example.com",
      "http://sub1.sub2.example.com" to "http://sub1.sub2.example.com",
    ),
    "authority with parameters" to listOf(
      "http://$v1.com?x=$v2" to "http://$v1.com?x=$v2queryEncoded"
    ),
    "ipv4" to listOf(
      "http://192.168.1.2/x" to "http://192.168.1.2/x",
      "http://abc/x" to "http://abc/x",
    ),
    "ipv6" to listOf(
      "http://[::1]/x" to "http://[::1]/x",
      "http://[1::3:4:5:6:7:8]/x" to "http://[1::3:4:5:6:7:8]/x",
      "http://[2001:0abcd:1bcde:2cdef::9f2e:0690:6969]/x" to "http://[2001:0abcd:1bcde:2cdef::9f2e:0690:6969]/x",
      "http://[::1]:8080/x" to "http://[::1]:8080/x",
      "http://[2001:0abcd:1bcde:2cdef::9f2e:0690:6969]/x" to "http://[2001:0abcd:1bcde:2cdef::9f2e:0690:6969]/x",
      "http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:8080" to "http://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:8080",
    ),
    "ports" to listOf(
      "http://example.com:8080" to "http://example.com:8080",
      "http://example.com:8080/x" to "http://example.com:8080/x",
      "http://example.com:/x" to "http://example.com/x",
    ),
    "path" to listOf(
      "http://example.com/$v1" to "http://example.com/$v1",
      "http://example.com/$v1/" to "http://example.com/$v1/",
      "http://example.com/$v2" to "http://example.com/$v2encoded",
      "http://example.com/$v2/$v1" to "http://example.com/$v2encoded/$v1",
      "http://example.com/a+b" to "http://example.com/a+b",
      "http://example.com/a%20b" to "http://example.com/a%20b"
    ),
    "path with parameters" to listOf(
      "http://example.com/$v1?x=$v2" to "http://example.com/$v1?x=$v2queryEncoded",
      "http://example.com/$v1/$v2?x=$v2" to "http://example.com/$v1/$v2encoded?x=$v2queryEncoded"
    ),
    "query parameter values" to listOf(
      "http://example.com?x=$v1" to "http://example.com?x=$v1",
      "http://example.com/?x=$v1" to "http://example.com/?x=$v1",
      "http://example.com?x=$v2" to "http://example.com?x=$v2queryEncoded",
      "http://example.com?x=$v1$v1" to "http://example.com?x=$v1$v1",
      "http://example.com?x=z$v1" to "http://example.com?x=z$v1",
      "http://example.com?x=a+b" to "http://example.com?x=a+b",
      "http://example.com?x=a=b" to "http://example.com?x=a%3Db",
      "http://example.com?x=a=b" to "http://example.com?x=a%3Db",
      "http://example.com?x=a%3Db&y=c%3Dd" to "http://example.com?x=a%3Db&y=c%3Dd"
    ),
    "query parameter without value" to listOf(
      "http://example.com?$v1" to "http://example.com?$v1",
      "http://example.com?$v1&$v2" to "http://example.com?$v1&$v2queryEncoded"
    ),
    "optional query parameters" to listOf(
      "http://example.com?a=$v1" to "http://example.com?a=$v1",
      "http://example.com?a=$v1&c=d" to "http://example.com?a=$v1&c=d"
    ),
    "fragments" to listOf(
      "http://example.com#$v1" to "http://example.com#$v1",
      "http://example.com#" to "http://example.com"
    ),
    "everything" to listOf(
      "http://$v1.$v2.com/$v1/$v2?$v1=$v2#$v1" to "http://$v1.$v2encoded.com/$v1/$v2encoded?$v1=$v2queryEncoded#$v1",
      "https://test user:pass@subdomain.domain.com:8080/my/path/../to/file.htm?subject=math&easy&problem=5-2=3=3&hello#hash_value" to
        "https://test%20user:pass@subdomain.domain.com:8080/my/path/../to/file.htm?subject=math&easy&problem=5-2%3D3%3D3&hello#hash_value"
    ),
    "embed whole url" to listOf(
      "${parseToUri("http://example.com:123/a").shouldBeRight()}/b/c" to "http://example.com:123/a/b/c",
      "${parseToUri("http://example.com/$v1?p=$v2").shouldBeRight()}" to "http://example.com/$v1?p=$v2queryEncoded"
    ),
    "encode unicode characters that are encoded as 3+ UTF-8 bytes" to listOf(
      "http://example.com/we/have/🍪" to "http://example.com/we/have/%F0%9F%8D%AA",
      "http://example.com/dont/run/with/✂" to "http://example.com/dont/run/with/%E2%9C%82",
      "http://example.com/in/query?key=🍪" to "http://example.com/in/query?key=%F0%9F%8D%AA",
      "http://example.com/in/query?🍪=value" to "http://example.com/in/query?%F0%9F%8D%AA=value"
    )
  )

  private val testTrimStart = listOf(
    "parse trim start Ascii white spaces" to listOf(
      " http://host/" to "http://host/",
      "\r\n \thttp://host/" to "http://host/",
    )
  )

  private val testDoesNotTrimOtherWhitespaceChars = listOf(
    "parse does not trim other whitespace characters" to listOf(
      "http://h/\u000b" to "/%0B",
      "http://h/\u001c" to "/%1C",
      "http://h/\u001d" to "/%1D",
      "http://h/\u001e" to "/%1E",
      "http://h/\u001f" to "/%1F",
      "http://h/\u0085" to "/%C2%85",
      "http://h/\u00a0" to "/%C2%A0",
    )
  )

  private val testScheme = listOf(
    "parse scheme" to listOf(
      "http://host/" to "http://host/",
      "Http://host/" to "http://host/",
      "HTTP://host/" to "http://host/",
      "https://host/" to "https://host/",
      "HTTPS://host/" to "https://host/",
      "nothttp://host/" to "Unexpected scheme: nothttp",
      "://host/" to "Unexpected scheme: ",
      "ht1tp://host/" to "Unexpected scheme: ht1tp",
      "httpss://host/" to "Unexpected scheme: httpss"
    )
  )

  init {
    for ((groupName, testCases: List<Pair<String, String>>) in testData) {
      for ((i: Int, pair: Pair<String, String>) in testCases.withIndex()) {
        test("[$groupName] should interpolate to ${pair.second} (${i + 1})") {
          val uri = parseToUri(pair.first).shouldBeRight()
          println("scheme=${uri.scheme}")
          println("Authority.userInfo=${uri.authority?.userInfo}")
          println("Authority.hostSegment=${uri.authority?.host()}")
          println("Authority.port=${uri.authority?.port}")
          println("pathSegments=${uri.pathSegments}")
          println("querySegments=${uri.querySegments}")
          println("fragmentSegment=${uri.fragment()}")
          println("uri.toString()=$uri")
          uri.toString() shouldBe pair.second
        }
      }
    }

    for ((groupName, testCases: List<Pair<String, String>>) in testTrimStart) {
      for ((i: Int, pair: Pair<String, String>) in testCases.withIndex()) {
        test("[$groupName] should interpolate to ${pair.second} (${i + 1})") {
          parseToUri(pair.first).shouldBeRight().toString() shouldBe pair.second
        }
      }
    }

    for ((groupName, testCases: List<Pair<String, String>>) in testDoesNotTrimOtherWhitespaceChars) {
      for ((i: Int, pair: Pair<String, String>) in testCases.withIndex()) {
        test("[$groupName] should interpolate to ${pair.second} (${i + 1})") {
          parseToUri(pair.first).shouldBeRight().pathSegments.toString() shouldBe pair.second
        }
      }
    }

    for ((groupName, testCases: List<Pair<String, String>>) in testScheme) {
      for ((i: Int, pair: Pair<String, String>) in testCases.withIndex()) {
        test("[$groupName] should interpolate to ${pair.second} (${i + 1})") {
          parseToUri(pair.first).fold(
            { uriError -> (uriError as UriError.UnexpectedScheme).errorMessage shouldBe pair.second },
            { it.toString() shouldBe pair.second }
          )
        }
      }
    }

    test("user name and password") {
      parseToUri("http://@host/path").shouldBeRight().toString().shouldBe("http://host/path")
      parseToUri("http://user@host/path").shouldBeRight().toString().shouldBe("http://user@host/path")
      parseToUri("http://user:pass@host/path").shouldBeRight().toString().shouldBe("http://user:pass@host/path")
      // the last @ is the delimiter
      parseToUri("http://foo@bar@baz/path").shouldBeRight().toString().shouldBe("http://foo%40bar@baz/path")
      parseToUri("http://username:@host/path").shouldBeRight().toString().shouldBe("http://username@host/path")
      // Chrome doesn't mind, but Firefox rejects URLs with empty usernames and non-empty passwords.
      // password with empty username and empty password
      parseToUri("http://:@host/path").shouldBeRight().toString().shouldBe("http://host/path")
      // password with empty username and some password
      parseToUri("http://:password@@host/path").shouldBeRight().apply {
        toString() shouldBe "http://host/path"
        authority?.userInfo.toString() shouldBe ":password%40"
      }
    }

    test("hostname characters") {
      parseToUri("http://\n/").shouldBeLeft().shouldBeTypeOf<UriError.InvalidHost>()
      parseToUri("http:// /").shouldBeLeft().shouldBeTypeOf<UriError.InvalidHost>()
      parseToUri("http://%20/").shouldBeLeft().shouldBeTypeOf<UriError.InvalidHost>()
      parseToUri("http://abcd").fold(
        { fail("this should work") },
        { UriCompatibility.encodeDNSHost(it.host().toString()) shouldBe "abcd" })
      parseToUri("http://ABCD").fold(
        { fail("this should work") },
        { UriCompatibility.encodeDNSHost(it.host().toString()) shouldBe "abcd" })
      parseToUri("http://σ")
        .fold({ fail("this should work") }, { UriCompatibility.encodeDNSHost(it.host().toString()) shouldBe "xn--4xa" })
      parseToUri("http://Σ")
        .fold({ fail("this should work") }, { UriCompatibility.encodeDNSHost(it.host().toString()) shouldBe "xn--4xa" })
      parseToUri("http://AB\u00ADCD")
        .fold({ fail("this should work") }, { UriCompatibility.encodeDNSHost(it.host().toString()) shouldBe "abcd" })
      parseToUri("http://\u2121")
        .fold({ fail("this should work") }, { UriCompatibility.encodeDNSHost(it.host().toString()) shouldBe "tel" })
      parseToUri("http://\uD87E\uDE1D").fold(
        { fail("this should work") },
        { UriCompatibility.encodeDNSHost(it.host().toString()) shouldBe "xn--pu5l" }
      )
    }

    test("hostname ipv6") {
      parseToUri("http://[::1]/")
        .fold({ fail("this should work") }, { it.host().toString() shouldBe "::1" })
      parseToUri("http://[::1]/").fold({ fail("this should work") }, { it.toString() shouldBe "http://[::1]/" })
      parseToUri("http://[::1]:8080/").fold({ fail("this should work") }, { it.port() shouldBe 8080 })
      parseToUri("http://user:password@[::1]/")
        .fold({ fail("this should work") }, { it.authority?.userInfo?.password shouldBe "password" })
      parseToUri("http://user:password@[::1]:8080/")
        .fold({ fail("this should work") }, { it.host().toString() shouldBe "::1" })
      parseToUri("http://[%3A%3A%31]/")
        .fold({ fail("this should work") }, { it.host().toString() shouldBe "::1" })
    }

    test("port") {
      parseToUri("http://host:80/").shouldBeRight().toString().shouldBe("http://host/")
      parseToUri("http://host:99/").shouldBeRight().toString().shouldBe("http://host:99/")
      parseToUri("http://host:/").shouldBeRight().toString().shouldBe("http://host/")
      parseToUri("http://host:65535/").shouldBeRight().port() shouldBe 65535
      parseToUri("http://host:0/").shouldBeLeft().shouldBeTypeOf<UriError.InvalidPort>()
      parseToUri("http://host:65536/").shouldBeLeft().shouldBeTypeOf<UriError.InvalidPort>()
      parseToUri("http://host:-1/").shouldBeLeft().shouldBeTypeOf<UriError.InvalidPort>()
      parseToUri("http://host:a/").shouldBeLeft().shouldBeTypeOf<UriError.InvalidPort>()
      parseToUri("http://host:%39%39/").shouldBeLeft().shouldBeTypeOf<UriError.InvalidPort>()
    }

    test("paths") {
      parseToUri("http://host/%00")
        .fold({ fail("this should work") }, { it.path() shouldContainExactly listOf("\u0000") })
      parseToUri("http://host/a/%E2%98%83/c")
        .fold({ fail("this should work") }, { it.path() shouldContainExactly listOf("a", "\u2603", "c") })
      parseToUri("http://host/a/%F0%9F%8D%A9/c")
        .fold({ fail("this should work") }, { it.path() shouldContainExactly listOf("a", "\uD83C\uDF69", "c") })
      parseToUri("http://host/a/%62/c")
        .fold({ fail("this should work") }, { it.path() shouldContainExactly listOf("a", "b", "c") })
      parseToUri("http://host/a/%7A/c")
        .fold({ fail("this should work") }, { it.path() shouldContainExactly listOf("a", "z", "c") })
      parseToUri("http://host/a/%7a/c")
        .fold({ fail("this should work") }, { it.path() shouldContainExactly listOf("a", "z", "c") })
      parseToUri("http://host/a%f/b")
        .fold({ it::class shouldBeSameInstanceAs UriError.IllegalArgument::class }, { fail("Expecting an error") })
      parseToUri("http://host/%/b")
        .fold({ it::class shouldBeSameInstanceAs UriError.IllegalArgument::class }, { fail("Expecting an error") })
      parseToUri("http://host/%")
        .fold({ it::class shouldBeSameInstanceAs UriError.IllegalArgument::class }, { fail("Expecting an error") })
      parseToUri("http://github.com/%%30%30")
        .fold({ it::class shouldBeSameInstanceAs UriError.IllegalArgument::class }, { fail("Expecting an error") })
    }

    test("[query parameter values] should interpolate correctly") {
      parseToUri("http://example.com?x=a=b")
        .fold(
          { fail("this should work") },
          {
            it.querySegmentsEncoding(QuerySegment.StandardValue)
              .toString() shouldBe "http://example.com?x=a=b"
          }
        )
      parseToUri("http://host/?a=!$(),/:;?@[]\\^`{|}~")
        .fold(
          { fail("this should work") },
          {
            it.querySegmentsEncoding(QuerySegment.All)
              .toString() shouldBe "http://host/?a=%21%24%28%29%2C%2F%3A%3B%3F%40%5B%5D%5C%5E%60%7B%7C%7D%7E"
          }
        )
    }

    test("[fragments] should interpolate correctly") {
      parseToUri("http://host/#=[]:;\"~|?#@^/$*").fold(
        { fail("this should work, error: $it") },
        { uri ->
          uri.fragmentSegmentEncoding { FragmentSegment.RelaxedWithBrackets(it) }
            .toString() shouldBe "http://host/#=[]:;%22~%7C?%23@%5E/$*"
        }
      )
    }
  }
}
