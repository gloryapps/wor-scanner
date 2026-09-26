package com.gloryapps.worscanner.scanner.text

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ValueUnit {
    @SerialName("percentage") PERCENTAGE,
    @SerialName("flat") FLAT,
}

/** A number as a card prints it: flat, or a percentage. */
data class Amount(val value: Double, val unit: ValueUnit)

/**
 * The numbers a row ends on: a row reads `<icon noise> <name> <value>`, so its numbers are the run
 * at its end. An `O` between digits is a zero (`1O50`), and letters glued to the last number are
 * an icon's noise (`830o`).
 */
fun numbersIn(row: String): List<Amount> {
    val read = row.replace(Regex("(?<=\\d)[Oo](?=\\d)"), "0").replace(Regex("(?<=\\d)[A-Za-z]+$"), "")
    val tail = Regex("[\\d.,%+\\s]+$").find(read)?.value ?: ""

    return Regex("(\\d+(?:[.,]\\d+)?)\\s*(%?)").findAll(tail).map { found ->
        Amount(found.groupValues[1].replace(',', '.').toDouble(), if (found.groupValues[2] == "%") ValueUnit.PERCENTAGE else ValueUnit.FLAT)
    }.toList()
}
