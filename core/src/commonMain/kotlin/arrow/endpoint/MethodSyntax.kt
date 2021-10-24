package arrow.endpoint

import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple6
import arrow.endpoint.Endpoint.Info
import arrow.endpoint.model.Method
import kotlin.jvm.JvmName

public interface MethodSyntax {

  public fun get(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) ArrowEndpoint.method(Method.GET).and(ArrowEndpoint.fixedPath(path))
      else ArrowEndpoint.method(Method.GET),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun post(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) ArrowEndpoint.method(Method.POST).and(ArrowEndpoint.fixedPath(path))
      else ArrowEndpoint.method(Method.POST),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun head(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) ArrowEndpoint.method(Method.HEAD).and(ArrowEndpoint.fixedPath(path))
      else ArrowEndpoint.method(Method.HEAD),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun put(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) ArrowEndpoint.method(Method.PUT).and(ArrowEndpoint.fixedPath(path))
      else ArrowEndpoint.method(Method.PUT),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun delete(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) ArrowEndpoint.method(Method.DELETE).and(ArrowEndpoint.fixedPath(path))
      else ArrowEndpoint.method(Method.DELETE),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun options(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) ArrowEndpoint.method(Method.OPTIONS).and(ArrowEndpoint.fixedPath(path))
      else ArrowEndpoint.method(Method.OPTIONS),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun patch(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) ArrowEndpoint.method(Method.PATCH).and(ArrowEndpoint.fixedPath(path))
      else ArrowEndpoint.method(Method.PATCH),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun connect(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) ArrowEndpoint.method(Method.CONNECT).and(ArrowEndpoint.fixedPath(path))
      else ArrowEndpoint.method(Method.CONNECT),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun trace(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) ArrowEndpoint.method(Method.TRACE).and(ArrowEndpoint.fixedPath(path))
      else ArrowEndpoint.method(Method.TRACE),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun <A> get(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      ArrowEndpoint.method(Method.GET).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun <A> post(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      ArrowEndpoint.method(Method.POST).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun <A> head(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      ArrowEndpoint.method(Method.HEAD).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun <A> put(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      ArrowEndpoint.method(Method.PUT).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun <A> delete(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      ArrowEndpoint.method(Method.DELETE).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun <A> options(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      ArrowEndpoint.method(Method.OPTIONS).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun <A> patch(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      ArrowEndpoint.method(Method.PATCH).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun <A> connect(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      ArrowEndpoint.method(Method.CONNECT).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )

  public fun <A> trace(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      ArrowEndpoint.method(Method.TRACE).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      Info.empty()
    )
}

// TODO split into interface/object if you want users to not be able to globally import `div`
public object PathSyntax {

  public operator fun String.div(other: String): EndpointInput<Unit> =
    ArrowEndpoint.fixedPath(this).and(ArrowEndpoint.fixedPath(other))

  public fun <A> p(name: String, codec: PlainCodec<A>): EndpointInput.PathCapture<A> =
    ArrowEndpoint.path(name, codec)

  public fun <A> p(codec: PlainCodec<A>): EndpointInput.PathCapture<A> =
    ArrowEndpoint.path(codec)

  public operator fun <A> String.div(other: EndpointInput<A>): EndpointInput<A> =
    ArrowEndpoint.fixedPath(this).and(other)

  public operator fun <A> EndpointInput<A>.div(other: String): EndpointInput<A> =
    and(ArrowEndpoint.fixedPath(other))

  @JvmName("andLeftUnit")
  public operator fun <A> EndpointInput<Unit>.div(other: EndpointInput<A>): EndpointInput<A> =
    this.and(other)

  public operator fun <A, B> EndpointInput<A>.div(other: EndpointInput<B>): EndpointInput<Pair<A, B>> =
    this.and(other)

  public operator fun <A> EndpointInput<A>.div(other: EndpointInput.FixedPath<Unit>): EndpointInput<A> =
    this.and(other)

  @JvmName("div2")
  public operator fun <A, B, C> EndpointInput<Pair<A, B>>.div(other: EndpointInput.PathCapture<C>): EndpointInput<Triple<A, B, C>> =
    this.and(other)

  @JvmName("div3")
  public operator fun <A, B, C, D> EndpointInput<Triple<A, B, C>>.div(other: EndpointInput.PathCapture<D>): EndpointInput<Tuple4<A, B, C, D>> =
    this.and(other)

  @JvmName("div4")
  public operator fun <A, B, C, D, E> EndpointInput<Tuple4<A, B, C, D>>.div(other: EndpointInput.PathCapture<E>): EndpointInput<Tuple5<A, B, C, D, E>> =
    this.and(other)

  @JvmName("div5")
  public operator fun <A, B, C, D, E, F> EndpointInput<Tuple5<A, B, C, D, E>>.div(other: EndpointInput.PathCapture<F>): EndpointInput<Tuple6<A, B, C, D, E, F>> =
    this.and(other)
}
