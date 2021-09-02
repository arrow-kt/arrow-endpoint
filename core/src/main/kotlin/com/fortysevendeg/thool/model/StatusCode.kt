package com.fortysevendeg.thool.model

import arrow.core.Either

@JvmInline
public value class StatusCode(public val code: Int) {
  public fun isInformational(): Boolean = code / 100 == 1
  public fun isSuccess(): Boolean = code / 100 == 2
  public fun isRedirect(): Boolean = code / 100 == 3
  public fun isClientError(): Boolean = code / 100 == 4
  public fun isServerError(): Boolean = code / 100 == 5

  override fun toString(): String = code.toString()

  public companion object {
    /** @throws IllegalArgumentException If the status code is out of range.
     */
    public fun unsafe(code: Int): StatusCode {
      check(code < 100 || code > 599) { "Status code outside of the allowed range 100-599: $code" }
      return StatusCode(code)
    }

    public fun safe(code: Int): Either<String, StatusCode> =
      if (code < 100 || code > 599) Either.Left("Status code outside of the allowed range 100-599: $code")
      else Either.Right(StatusCode(code))

    // https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml
    public val Continue: StatusCode = StatusCode(100)
    public val SwitchingProtocols: StatusCode = StatusCode(101)
    public val Processing: StatusCode = StatusCode(102)
    public val EarlyHints: StatusCode = StatusCode(103)

    public val Ok: StatusCode = StatusCode(200)
    public val Created: StatusCode = StatusCode(201)
    public val Accepted: StatusCode = StatusCode(202)
    public val NonAuthoritativeInformation: StatusCode = StatusCode(203)
    public val NoContent: StatusCode = StatusCode(204)
    public val ResetContent: StatusCode = StatusCode(205)
    public val PartialContent: StatusCode = StatusCode(206)
    public val MultiStatus: StatusCode = StatusCode(207)
    public val AlreadyReported: StatusCode = StatusCode(208)
    public val ImUsed: StatusCode = StatusCode(226)

    public val MultipleChoices: StatusCode = StatusCode(300)
    public val MovedPermanently: StatusCode = StatusCode(301)
    public val Found: StatusCode = StatusCode(302)
    public val SeeOther: StatusCode = StatusCode(303)
    public val NotModified: StatusCode = StatusCode(304)
    public val UseProxy: StatusCode = StatusCode(305)
    public val TemporaryRedirect: StatusCode = StatusCode(307)
    public val PermanentRedirect: StatusCode = StatusCode(308)

    public val BadRequest: StatusCode = StatusCode(400)
    public val Unauthorized: StatusCode = StatusCode(401)
    public val PaymentRequired: StatusCode = StatusCode(402)
    public val Forbidden: StatusCode = StatusCode(403)
    public val NotFound: StatusCode = StatusCode(404)
    public val MethodNotAllowed: StatusCode = StatusCode(405)
    public val NotAcceptable: StatusCode = StatusCode(406)
    public val ProxyAuthenticationRequired: StatusCode = StatusCode(407)
    public val RequestTimeout: StatusCode = StatusCode(408)
    public val Conflict: StatusCode = StatusCode(409)
    public val Gone: StatusCode = StatusCode(410)
    public val LengthRequired: StatusCode = StatusCode(411)
    public val PreconditionFailed: StatusCode = StatusCode(412)
    public val PayloadTooLarge: StatusCode = StatusCode(413)
    public val UriTooLong: StatusCode = StatusCode(414)
    public val UnsupportedMediaType: StatusCode = StatusCode(415)
    public val RangeNotSatisfiable: StatusCode = StatusCode(416)
    public val ExpectationFailed: StatusCode = StatusCode(417)
    public val MisdirectedRequest: StatusCode = StatusCode(421)
    public val UnprocessableEntity: StatusCode = StatusCode(422)
    public val Locked: StatusCode = StatusCode(423)
    public val FailedDependency: StatusCode = StatusCode(424)
    public val UpgradeRequired: StatusCode = StatusCode(426)
    public val PreconditionRequired: StatusCode = StatusCode(428)
    public val TooManyRequests: StatusCode = StatusCode(429)
    public val RequestHeaderFieldsTooLarge: StatusCode = StatusCode(431)
    public val UnavailableForLegalReasons: StatusCode = StatusCode(451)

    public val InternalServerError: StatusCode = StatusCode(500)
    public val NotImplemented: StatusCode = StatusCode(501)
    public val BadGateway: StatusCode = StatusCode(502)
    public val ServiceUnavailable: StatusCode = StatusCode(503)
    public val GatewayTimeout: StatusCode = StatusCode(504)
    public val HttpVersionNotSupported: StatusCode = StatusCode(505)
    public val VariantAlsoNegotiates: StatusCode = StatusCode(506)
    public val InsufficientStorage: StatusCode = StatusCode(507)
    public val LoopDetected: StatusCode = StatusCode(508)
    public val NotExtended: StatusCode = StatusCode(510)
    public val NetworkAuthenticationRequired: StatusCode = StatusCode(511)
  }
}
