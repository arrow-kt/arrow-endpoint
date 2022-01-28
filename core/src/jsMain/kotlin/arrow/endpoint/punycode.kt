package arrow.endpoint

@JsModule("punycode")
@JsNonModule
@JsName("toASCII")
internal external val toASCII: (domain: String) -> String
