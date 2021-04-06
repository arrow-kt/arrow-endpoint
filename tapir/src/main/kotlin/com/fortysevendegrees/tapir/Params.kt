package com.fortysevendegrees.tapir

import arrow.core.Tuple10
import arrow.core.Tuple11
import arrow.core.Tuple12
import arrow.core.Tuple13
import arrow.core.Tuple14
import arrow.core.Tuple15
import arrow.core.Tuple16
import arrow.core.Tuple17
import arrow.core.Tuple18
import arrow.core.Tuple19
import arrow.core.Tuple20
import arrow.core.Tuple21
import arrow.core.Tuple22
import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple6
import arrow.core.Tuple7
import arrow.core.Tuple8
import arrow.core.Tuple9

/**
 * A union type: Unit | value | Tuple. Represents the possible parameters of an endpoint's input/output:
 * no parameters, a single parameter (a "stand-alone" value instead of a 1-tuple), and multiple parameters.
 *
 * There are two views on parameters: [ParamsAsAny], where the parameters are represented as instances of
 * the union type, or [ParamsAsList], where the parameters are represented as a vector of size 0/1/2+.
 */
sealed class Params {
  abstract val asAny: Any?
  abstract val asList: List<Any?>

  object Unit : Params() {
    override val asAny: Any = Unit
    override val asList: List<Any?> = emptyList()
  }

  data class ParamsAsAny(override val asAny: Any?) : Params() {
    override val asList: List<Any?> by lazy { toParams(asAny) }

    private fun toParams(a: Any?): List<Any?> =
      when (a) {
        is Pair<*, *> -> listOf(a.first, a.second)
        is Triple<*, *, *> -> listOf(a.first, a.second, a.third)
        is Tuple4<*, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth)
        is Tuple5<*, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth)
        is Tuple6<*, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth)
        is Tuple7<*, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh)
        is Tuple8<*, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth)
        is Tuple9<*, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth)
        is Tuple10<*, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth)
        is Tuple11<*, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh)
        is Tuple12<*, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth)
        is Tuple13<*, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth)
        is Tuple14<*, *, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth, a.fourteenth)
        is Tuple15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth, a.fourteenth, a.fifteenth)
        is Tuple16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth, a.fourteenth, a.fifteenth, a.sixteenth)
        is Tuple17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth, a.fourteenth, a.fifteenth, a.sixteenth, a.seventeenth)
        is Tuple18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth, a.fourteenth, a.fifteenth, a.sixteenth, a.seventeenth, a.eighteenth)
        is Tuple19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth, a.fourteenth, a.fifteenth, a.sixteenth, a.seventeenth, a.eighteenth, a.nineteenth)
        is Tuple20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth, a.fourteenth, a.fifteenth, a.sixteenth, a.seventeenth, a.eighteenth, a.nineteenth, a.twentieth)
        is Tuple21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth, a.fourteenth, a.fifteenth, a.sixteenth, a.seventeenth, a.eighteenth, a.nineteenth, a.twentieth, a.twentyFirst)
        is Tuple22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *> -> listOf(a.first, a.second, a.third, a.fourth, a.fifth, a.sixth, a.seventh, a.eighth, a.ninth, a.tenth, a.eleventh, a.twelfth, a.thirteenth, a.fourteenth, a.fifteenth, a.sixteenth, a.seventeenth, a.eighteenth, a.nineteenth, a.twentieth, a.twentyFirst, a.twentySecond)
        else -> listOf(a)
      }
  }

  data class ParamsAsList(override val asList: List<Any?>) : Params() {
    override val asAny: Any? by lazy { toParams(asList) }

    private fun <T> toParams(seq: List<T>): Any? =
      when (seq.size) {
        0 -> kotlin.Unit
        1 -> seq[0]
        2 -> Pair(seq[0], seq[1])
        3 -> Triple(seq[0], seq[1], seq[2])
        4 -> Tuple4(seq[0], seq[1], seq[2], seq[3])
        5 -> Tuple5(seq[0], seq[1], seq[2], seq[3], seq[4])
        6 -> Tuple6(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5])
        7 -> Tuple7(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6])
        8 -> Tuple8(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7])
        9 -> Tuple9(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8])
        10 -> Tuple10(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9])
        11 -> Tuple11(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10])
        12 -> Tuple12(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11])
        13 -> Tuple13(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12])
        14 -> Tuple14(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12], seq[13])
        15 -> Tuple15(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12], seq[13], seq[14])
        16 -> Tuple16(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12], seq[13], seq[14], seq[15])
        17 -> Tuple17(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12], seq[13], seq[14], seq[15], seq[16])
        18 -> Tuple18(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12], seq[13], seq[14], seq[15], seq[16], seq[17])
        19 -> Tuple19(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12], seq[13], seq[14], seq[15], seq[16], seq[17], seq[18])
        20 -> Tuple20(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12], seq[13], seq[14], seq[15], seq[16], seq[17], seq[18], seq[19])
        21 -> Tuple21(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12], seq[13], seq[14], seq[15], seq[16], seq[17], seq[18], seq[19], seq[20])
        22 -> Tuple22(seq[0], seq[1], seq[2], seq[3], seq[4], seq[5], seq[6], seq[7], seq[8], seq[9], seq[10], seq[11], seq[12], seq[13], seq[14], seq[15], seq[16], seq[17], seq[18], seq[19], seq[20], seq[21])
        else -> throw IllegalArgumentException("Cannot convert $seq to params!")
      }
  }
}

typealias CombineParams = (Params, Params) -> Params
typealias SplitParams = (Params) -> Pair<Params, Params>
