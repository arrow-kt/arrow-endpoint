[![Kotlin Experimental](https://kotl.in/badges/experimental.svg)](https://kotlinlang.org/docs/components-stability.html)
# Arrow Endpoint

Arrow Endpoint offers a composable `Endpoint` datatype, that allows us easily define an `Endpoint` from which we can derive
clients, servers & documentation.

## Rationale

When working on functional applications we care about function signatures, in the domain of endpoints this boils down
to `suspend (Input) -> Either<Error, Output>`.

This signature models any endpoint that takes an `Input`, and returns with either a domain `Error`, an `Output` or an
unknown `Throwbale` from the `suspend` side effect. Where `Error` means a return with `StatusCode` **outside** of
the `2xx` range

So we want to be able to work with `Endpoint` as such. So given the definition of an `Endpoint<Input, Error, Output>` we
want to be able to:

- Derive a client with shape `suspend (Input) -> Either<Error, Output>`
- Derive a server that runs `suspend (Input) -> Either<Error, Output>`
- Provide http documentation in the desired shape
