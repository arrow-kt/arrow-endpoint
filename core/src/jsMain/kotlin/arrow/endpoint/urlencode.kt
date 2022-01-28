package arrow.endpoint

@JsModule("urlencode")
@JsNonModule
internal external fun encode(str: String, charset: String): String = definedExternally

@JsModule("urlencode")
@JsNonModule
internal external fun decode(str: String, charset: String): String = definedExternally

