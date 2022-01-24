package arrow.endpoint

import arrow.core.tail
import io.ktor.utils.io.charsets.Charset

internal expect object UrlencodedData {
  fun decode(s: String, charset: Charset): List<Pair<String, String>>

  fun encode(s: List<Pair<String, String>>, charset: Charset): String
}

internal fun <A, B> Set<A>.map(transform: (A) -> B): Set<B> {
  val destination = mutableSetOf<B>()
  for (item in this)
    destination.add(transform(item))
  return destination
}

internal fun <E> Iterable<E>.updated(index: Int, elem: E): List<E> =
  mapIndexed { i, existing -> if (i == index) elem else existing }

internal fun basicInputSortIndex(i: EndpointInput.Basic<*, *, *>): Int =
  when (i) {
    is EndpointInput.FixedMethod<*> -> 0
    is EndpointInput.FixedPath<*> -> 1
    is EndpointInput.PathCapture<*> -> 1
    is EndpointInput.PathsCapture<*> -> 1
    is EndpointInput.Query<*> -> 2
    is EndpointInput.QueryParams<*> -> 2
    is EndpointInput.Cookie<*> -> 3
    is EndpointIO.Header<*> -> 3
//  is EndpointIO.Headers<*> -> 3
//  is EndpointIO.FixedHeader<*> -> 3
//  is EndpointInput.ExtractFromRequest<*> -> 4
    is EndpointIO.Body<*, *> -> 6
    is EndpointIO.Empty<*> -> 7
  }

internal fun <A> List<A>.initAndLastOrNull(): Pair<List<A>, A>? =
  if (isEmpty()) null else Pair(dropLast(1), last())

internal fun <A> List<A>.headAndTailOrNull(): Pair<A, List<A>>? =
  if (isEmpty()) null else Pair(first(), tail())
