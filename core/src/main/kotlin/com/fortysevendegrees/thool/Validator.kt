package com.fortysevendegrees.thool

import com.fortysevendegrees.thool.Validator.Single.Primitive.Max
import com.fortysevendegrees.thool.Validator.Single.Primitive.MaxLength
import com.fortysevendegrees.thool.Validator.Single.Primitive.MaxSize
import com.fortysevendegrees.thool.Validator.Single.Primitive.Min
import com.fortysevendegrees.thool.Validator.Single.Primitive.MinLength
import com.fortysevendegrees.thool.Validator.Single.Primitive.MinSize
import com.fortysevendegrees.thool.Validator.Single.Primitive.Pattern
import com.fortysevendegrees.thool.Validator.Single.CollectionElements
import com.fortysevendegrees.thool.Validator.Single.Mapped
import com.fortysevendegrees.thool.Validator.Single.Primitive
import com.fortysevendegrees.thool.Validator.Single.Custom
import arrow.core.Option

sealed class Validator<A> {
  abstract fun validate(t: A): List<ValidationError<*>>

  open fun <B> contramap(g: (B) -> A): Validator<B> = Mapped(this, g)

  open fun asOptionElement(): Validator<Option<A>> = CollectionElements(this).contramap { it.toList() }
  open fun asNullableElement(): Validator<A?> =
    CollectionElements(this).contramap { it?.let { listOf(it) } ?: emptyList() }

  open fun asArrayElements(): Validator<Array<A>> = CollectionElements(this).contramap { it.toList() }
  open fun asListElements(): Validator<List<A>> = CollectionElements(this).contramap { it }

  open fun and(other: Validator<A>): Validator<A> = all(this, other)
  open fun or(other: Validator<A>): Validator<A> = any(this, other)

//  fun show: Option[String] = com.fortysevendegrees.thool.Validator.show(this)

  companion object {
    fun <A> all(vararg v: Validator<A>): Validator<A> =
      if (v.size == 1) v.first() else All(v.toList())

    fun <A> any(vararg v: Validator<A>): Validator<A> =
      if (v.size == 1) v.first() else Any(v.toList())

    private val _pass: Validator<Nothing> = all()
    private val _reject: Validator<Nothing> = any()

    /** A validator instance that always pass. */
    fun <A> pass(): Validator<A> = _pass as Validator<A>

    /** A validator instance that always reject. */
    fun <A> reject(): Validator<A> = _reject as Validator<A>

    fun <A : Comparable<A>> min(value: A, exclusive: Boolean = false): Primitive<A> =
      Min(value, exclusive)

    fun <A : Comparable<A>> max(value: A, exclusive: Boolean = false): Primitive<A> =
      Max(value, exclusive)

    fun pattern(value: Regex): Primitive<String> =
      Pattern(value)

    fun minLength(value: Int): Primitive<String> =
      MinLength(value)

    fun maxLength(value: Int): Primitive<String> =
      MaxLength(value)

    fun <A> minSize(value: Int): Primitive<List<A>> =
      MinSize(value)

    fun <A> maxSize(value: Int): Primitive<List<A>> =
      MaxSize(value)

    fun <A> custom(doValidate: (A) -> List<ValidationError<*>>, showMessage: String? = null): Validator<A> =
      Custom(doValidate, showMessage)

    /**
     * Creates an enum validator
     * This enumeration will only be used for documentation, as a value outside of the allowed values will not be
     * decoded in the first place (the decoder has no other option than to fail).
     */
    inline fun <reified A : Enum<A>> enum(): Primitive.Enum<A> =
      Primitive.Enum(enumValues(), null)

    /**
     * Creates an enum validator
     * This enumeration will only be used for documentation, as a value outside of the allowed values will not be
     * decoded in the first place (the decoder has no other option than to fail).
     */
    inline fun <reified A : Enum<A>> enum(noinline encode: (A) -> kotlin.Any?): Primitive.Enum<A> =
      Primitive.Enum(enumValues(), encode)

    fun <A> enum(possibleValues: Array<A>): Primitive.Enum<A> =
      Primitive.Enum(possibleValues, null)

    /**
     * @param encode Specify how values of this type can be encoded to a raw value, which will be used for documentation.
     *               This will be automatically inferred if the validator is directly added to a codec.
     */
    fun <A> enum(possibleValues: Array<A>, encode: (A) -> kotlin.Any?): Primitive.Enum<A> =
      Primitive.Enum(possibleValues, encode)

//  fun openProduct[V](elemValidator: com.fortysevendegrees.thool.Validator[V]): com.fortysevendegrees.thool.Validator[Map[String, V]] =
//  if (elemValidator == pass) pass else OpenProduct(elemValidator)
  }

  data class All<A>(val validators: List<Validator<A>>) : Validator<A>() {
    override fun validate(t: A): List<ValidationError<*>> =
      validators.flatMap { it.validate(t) }.toList()

    override fun <B> contramap(g: (B) -> A): Validator<B> =
      if (validators.isEmpty()) All(emptyList()) else super.contramap(g)

    override fun and(other: Validator<A>): Validator<A> =
      if (validators.isEmpty()) other else All(validators + other)

    override fun asOptionElement(): Validator<Option<A>> =
      if (validators.isEmpty()) All(emptyList()) else super.asOptionElement()

    override fun asArrayElements(): Validator<Array<A>> =
      if (validators.isEmpty()) All(emptyList()) else super.asArrayElements()

    override fun asListElements(): Validator<List<A>> =
      if (validators.isEmpty()) All(emptyList()) else super.asListElements()
  }

  data class Any<A>(val validators: List<Validator<A>>) : Validator<A>() {
    override fun validate(t: A): List<ValidationError<*>> {
      val results = validators.map { it.validate(t) }
      return if (results.any { it.isEmpty() }) emptyList()
      else results.flatten()
    }

    override fun <B> contramap(g: (B) -> A): Validator<B> =
      if (validators.isEmpty()) Any(emptyList()) else super.contramap(g)

    override fun or(other: Validator<A>): Validator<A> =
      if (validators.isEmpty()) other else Any(validators + other)
  }

  sealed class Single<A> : Validator<A>() {
    data class Custom<A>(val doValidate: (A) -> List<ValidationError<*>>, val showMessage: String? = null) :
      Single<A>() {
      override fun validate(t: A): List<ValidationError<*>> =
        doValidate(t)
    }

    data class Mapped<A, B>(val wrapped: Validator<A>, val transform: (B) -> A) : Validator<B>() {
      override fun validate(t: B): List<ValidationError<*>> =
        wrapped.validate(transform(t))
    }

    data class CollectionElements<E>(val elementValidator: Validator<E>) : Single<Iterable<E>>() {
      override fun validate(t: Iterable<E>): List<ValidationError<*>> =
        t.flatMap(elementValidator::validate)
    }

    sealed class Primitive<A> : Single<A>() {

      data class Min<A : Comparable<A>>(val value: A, val exclusive: Boolean) : Primitive<A>() {
        override fun validate(t: A): List<ValidationError<*>> =
          if (t > value || !exclusive && t == value) emptyList()
          else listOf(ValidationError.Primitive(this, t))
      }

      data class Max<A : Comparable<A>>(val value: A, val exclusive: Boolean) : Primitive<A>() {
        override fun validate(t: A): List<ValidationError<*>> =
          if (t < value || !exclusive && t == value) emptyList()
          else listOf(ValidationError.Primitive(this, t))
      }

      data class Pattern(val value: Regex) : Primitive<String>() {
        override fun validate(t: String): List<ValidationError<*>> =
          if (t.matches(value)) emptyList()
          else listOf(ValidationError.Primitive(this, t))
      }

      data class MinLength(val value: Int) : Primitive<String>() {
        override fun validate(t: String): List<ValidationError<*>> =
          if (t.length >= value) emptyList()
          else listOf(ValidationError.Primitive(this, t))
      }

      data class MaxLength(val value: Int) : Primitive<String>() {
        override fun validate(t: String): List<ValidationError<*>> =
          if (t.length <= value) emptyList()
          else listOf(ValidationError.Primitive(this, t))
      }

      data class MinSize<A>(val value: Int) : Primitive<List<A>>() {
        override fun validate(t: List<A>): List<ValidationError<*>> =
          if (t.size >= value) emptyList()
          else listOf(ValidationError.Primitive(this, t))
      }

      data class MaxSize<A>(val value: Int) : Primitive<List<A>>() {
        override fun validate(t: List<A>): List<ValidationError<*>> =
          if (t.size <= value) emptyList()
          else listOf(ValidationError.Primitive(this, t))
      }

      data class Enum<A>(val possibleValues: Array<A>, val encode: ((A) -> kotlin.Any?)?) : Primitive<A>() {
        override fun validate(t: A): List<ValidationError<*>> =
          if (possibleValues.contains(t)) emptyList()
          else listOf(ValidationError.Primitive(this, t))

        /**
         * Specify how values of this type can be encoded to a raw value (typically a [String]).
         * This encoding will be used when generating documentation.
         */
        fun encode(e: (A) -> kotlin.Any?): Enum<A> = copy(encode = e)
      }
    }
  }

  fun show(): String? =
    when (this) {
      is Min -> "${if (exclusive) ">" else ">="}$value"
      is Max -> "${if (exclusive) "<" else "<="}$value"
      is Pattern -> "~$value"
      is MinLength -> "length>=$value"
      is MaxLength -> "length<=$value"
      is MinSize<*> -> "size>=$value"
      is MaxSize<*> -> "size<=$value"
      is Custom -> showMessage ?: "custom"
      is Primitive.Enum -> "in(${possibleValues.joinToString(",")}"
      is CollectionElements<*> -> elementValidator.show()?.let { "elements($it)" }
      is Mapped<*, *> -> wrapped.toString()
      is All<*> -> validators.takeIf { it.isNotEmpty() }
        ?.joinToString(",", prefix = "all(", postfix = ")") { it.toString() }
      is Any<*> -> validators.takeIf { it.isNotEmpty() }
        ?.joinToString(",", prefix = "any(", postfix = ")") { it.toString() } ?: "reject"
    }
}

// object com.fortysevendegrees.thool.Validator extends ValidatorEnumMacro {
//  // Used to capture encoding of a value to a raw format, which will then be directly rendered as a string in
//  // documentation. This is needed as codecs for nested types aren't available.
//  type EncodeToRaw < A > = T => Option[scala.Any]
//
//  case class OpenProduct[E](elementValidator: com.fortysevendegrees.thool.Validator[E]) extends Single[Map[String, E]] {
//    override fun validate(t: Map[String, E]): List[com.fortysevendegrees.thool.ValidationError[_]] = {
//    t.flatMap { case(name, value) => elementValidator.validate(value).com.fortysevendegrees.thool.map(_.prependPath(com.fortysevendegrees.thool.FieldName(name, name))) }
//  }.toList
//  }

//
//  /** A reference to a recursive validator. Should be set once during construction of the validator tree.
//   */
//  case class Ref<A>(private var _wrapped: com.fortysevendegrees.thool.Validator<A>) extends com.fortysevendegrees.thool.Validator<A> {
//    fun set(w: com.fortysevendegrees.thool.Validator<A>): this.type = {
//    if (_wrapped == null) _wrapped =
//      w else throw new IllegalArgumentException (s"com.fortysevendegrees.thool.Validator reference is already set to ${_wrapped}!")
//    this
//  }
//    fun wrapped: com.fortysevendegrees.thool.Validator<A> = {
//      if (_wrapped == null) throw new IllegalStateException ("No recursive validator reference set!")
//      _wrapped
//    }
//
//    override fun validate(t: T): List[com.fortysevendegrees.thool.ValidationError[_]] = wrapped.validate(t)
//  }
//  object Ref {
//    fun apply<A>(): Ref<A> = Ref(null)
//  }
//
//  //
//
//  fun show<A>(v: com.fortysevendegrees.thool.Validator<A>): Option[String] = {
//    v match {
//      case Min (value, exclusive)     => Some(s"${if (exclusive) ">" else ">="}$value")
//      case Max (value, exclusive)     => Some(s"${if (exclusive) "<" else "<="}$value")
//      case Pattern (value)            => Some(s"~$value")
//      case MinLength (value)          => Some(s"length>=$value")
//      case MaxLength (value)          => Some(s"length<=$value")
//      case MinSize (value)            => Some(s"size>=$value")
//      case MaxSize (value)            => Some(s"size<=$value")
//      case Custom (_, showMessage)    => showMessage.orElse(Some("custom"))
//      case Enum (possibleValues, _)   => Some(s"in(${possibleValues.mkString(",")}")
//      case CollectionElements (el, _) => show(el).com.fortysevendegrees.thool.map(se => s"elements($se)")
//      case Product (fields) =>
//      fields.flatMap {
//        case(n, f) =>
//        show(f.validator).com.fortysevendegrees.thool.map(n -> _)
//      }.toList match {
//        case Nil => None
//          case l => Some (l.com.fortysevendegrees.thool.map { case(n, s) => s"$n->($s)" }.mkString(","))
//      }
//      case c @ Coproduct(_) =>
//      c.subtypes.flatMap {
//        case(n, v) =>
//        show(v).com.fortysevendegrees.thool.map(n -> _)
//      }.toList match {
//        case Nil => None
//          case l => Some (l.com.fortysevendegrees.thool.map { case(n, s) => s"$n->($s)" }.mkString(","))
//      }
//      case OpenProduct (el)    => show(el).com.fortysevendegrees.thool.map(se => s"elements($se)")
//      case Mapped (wrapped, _) => show(wrapped)
//      case All (validators) =>
//      validators.flatMap(show(_)) match {
//        case immutable . Seq ()  => None
//        case immutable . Seq (s) => Some(s)
//        case ss => Some (s"all(${ss.mkString(",")})")
//      }
//      case Any (validators) =>
//      validators.flatMap(show(_)) match {
//        case immutable . Seq ()  => Some("reject")
//        case immutable . Seq (s) => Some(s)
//        case ss => Some (s"any(${ss.mkString(",")})")
//      }
//      case Ref (_) => Some("recursive")
//    }
//  }
//  }

sealed class ValidationError<A> {
  abstract fun prependPath(f: FieldName): ValidationError<A>
  abstract val invalidValue: A
  abstract val path: List<FieldName>

  data class Primitive<A>(
    val validator: Validator.Single.Primitive<A>,
    override val invalidValue: A,
    override val path: List<FieldName> = emptyList()
  ) :
    ValidationError<A>() {
    override fun prependPath(f: FieldName): ValidationError<A> = copy(path = listOf(f) + path)
  }

  data class Custom<A>(
    override val invalidValue: A,
    val message: String,
    override val path: List<FieldName> = emptyList()
  ) :
    ValidationError<A>() {
    override fun prependPath(f: FieldName): ValidationError<A> = copy(path = listOf(f) + path)
  }
}
