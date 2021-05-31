package com.fortysevendeg.thool.dsl

import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple6
import com.fortysevendeg.thool.Endpoint
import com.fortysevendeg.thool.EndpointInfo
import com.fortysevendeg.thool.EndpointInput
import com.fortysevendeg.thool.EndpointOutput
import com.fortysevendeg.thool.Params
import com.fortysevendeg.thool.PlainCodec
import com.fortysevendeg.thool.Thool
import com.fortysevendeg.thool.and
import com.fortysevendeg.thool.model.Method

public interface MethodSyntax {

  fun get(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) Thool.method(Method.GET).and(Thool.fixedPath(path))
      else Thool.method(Method.GET),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun post(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) Thool.method(Method.POST).and(Thool.fixedPath(path))
      else Thool.method(Method.POST),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun head(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) Thool.method(Method.HEAD).and(Thool.fixedPath(path))
      else Thool.method(Method.HEAD),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun put(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) Thool.method(Method.PUT).and(Thool.fixedPath(path))
      else Thool.method(Method.PUT),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun delete(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) Thool.method(Method.DELETE).and(Thool.fixedPath(path))
      else Thool.method(Method.DELETE),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun options(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) Thool.method(Method.OPTIONS).and(Thool.fixedPath(path))
      else Thool.method(Method.OPTIONS),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun patch(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) Thool.method(Method.PATCH).and(Thool.fixedPath(path))
      else Thool.method(Method.PATCH),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun connect(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) Thool.method(Method.CONNECT).and(Thool.fixedPath(path))
      else Thool.method(Method.CONNECT),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun trace(path: String? = null): Endpoint<Unit, Unit, Unit> =
    Endpoint(
      if (path != null) Thool.method(Method.TRACE).and(Thool.fixedPath(path))
      else Thool.method(Method.TRACE),
      EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun <A> get(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      Thool.method(Method.GET).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun <A> post(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      Thool.method(Method.POST).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun <A> head(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      Thool.method(Method.HEAD).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun <A> put(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      Thool.method(Method.PUT).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun <A> delete(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      Thool.method(Method.DELETE).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun <A> options(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      Thool.method(Method.OPTIONS).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun <A> patch(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      Thool.method(Method.PATCH).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun <A> connect(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      Thool.method(Method.CONNECT).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )

  fun <A> trace(f: PathSyntax.() -> EndpointInput<A>): Endpoint<A, Unit, Unit> =
    Endpoint(
      Thool.method(Method.TRACE).and(f(PathSyntax)), EndpointOutput.empty(),
      EndpointOutput.empty(),
      EndpointInfo.empty()
    )
}

// TODO split into interface/object if you want users to not be able to globally import `div`
object PathSyntax {

  operator fun String.div(other: String): EndpointInput<Unit> =
    Thool.fixedPath(this).and(Thool.fixedPath(other))

  fun <A> p(name: String, codec: PlainCodec<A>): EndpointInput.PathCapture<A> =
    Thool.path(name, codec)

  fun <A> p(codec: PlainCodec<A>): EndpointInput.PathCapture<A> =
    Thool.path(codec)

  operator fun <A> String.div(other: EndpointInput<A>): EndpointInput<A> =
    Thool.fixedPath(this).and(other)

  operator fun <A> EndpointInput<A>.div(other: String): EndpointInput<A> =
    and(Thool.fixedPath(other))

  @JvmName("andLeftUnit")
  operator fun <A> EndpointInput<Unit>.div(other: EndpointInput<A>): EndpointInput<A> =
    this.and(other)

  operator fun <A, B> EndpointInput<A>.div(other: EndpointInput<B>): EndpointInput<Pair<A, B>> =
    this.and(other)

  operator fun <A> EndpointInput<A>.div(other: EndpointInput.FixedPath<Unit>): EndpointInput<A> =
    EndpointInput.Pair(
      this,
      other,
      { p1, _ -> p1 },
      { p -> Pair(p, Params.Unit) }
    )

  @JvmName("div2")
  operator fun <A, B, C> EndpointInput<Pair<A, B>>.div(other: EndpointInput.PathCapture<C>): EndpointInput<Triple<A, B, C>> =
    this.and(other)

  @JvmName("div3")
  operator fun <A, B, C, D> EndpointInput<Triple<A, B, C>>.div(other: EndpointInput.PathCapture<D>): EndpointInput<Tuple4<A, B, C, D>> =
    this.and(other)

  @JvmName("div4")
  operator fun <A, B, C, D, E> EndpointInput<Tuple4<A, B, C, D>>.div(other: EndpointInput.PathCapture<E>): EndpointInput<Tuple5<A, B, C, D, E>> =
    this.and(other)

  @JvmName("div5")
  operator fun <A, B, C, D, E, F> EndpointInput<Tuple5<A, B, C, D, E>>.div(other: EndpointInput.PathCapture<F>): EndpointInput<Tuple6<A, B, C, D, E, F>> =
    this.and(other)
}
