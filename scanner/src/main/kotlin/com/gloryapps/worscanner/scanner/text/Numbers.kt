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

/** The numbers a row ends on: a row reads `<icon noise> <name> <value>`, so its numbers are the run at its end. */
fun numbersIn(row: String): List<Amount> {
    val tail = Regex("[\\d.,%+\\s]+$").find(row)?.value ?: ""

    return Regex("(\\d+(?:[.,]\\d+)?)\\s*(%?)").findAll(tail).map { found ->
        Amount(found.groupValues[1].replace(',', '.').toDouble(), if (found.groupValues[2] == "%") ValueUnit.PERCENTAGE else ValueUnit.FLAT)
    }.toList()
}
