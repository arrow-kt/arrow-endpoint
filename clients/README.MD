# Client libraries

The client libraries offer integrations with different frameworks such that you can use [Endpoint]
as a definition for making HTTP requests.

The clients offer a unified API for all frameworks of the following 2 base signatures:

```kotlin
typealias FrameworkClient = ...
typealias FrameworkRequest = ...
typealias FrameworkResponsee = ...

suspend operator fun <I, E, O> FrameworkClient.invoke(endpoint: Endpoint<I, E, O>, baseUrl: String, input: I): DecodeResult<Either<E, O>>

suspend fun <I, E, O> FrameworkClient.execute(endpoint: Endpoint<I, E, O>, baseUrl: String, input: I): Triple<FrameworkRequest, FrameworkResponse, DecodeResult<Either<E, O>>>
```

Where FrameworkClient is:
- `HttpHandler` for Http4k
- `HttpClient` for Ktor
- `RestTemplate` for Spring
- `WebClient` for Spring WebFlux

You can extract `Either<E, O>` from `DecodeResult` by using `getOrThrow()`, `getOrNull()` or by pattern matching over the `DecodeResult` ADT.
