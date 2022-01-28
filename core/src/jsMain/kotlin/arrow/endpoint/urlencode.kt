package arrow.endpoint

@JsModule("urlencode")
@JsNonModule
internal external fun encode(str: String, char: String): String = definedExternally

@JsModule("urlencode")
@JsNonModule
internal external fun decode(str: String, char: String): String = definedExternally

