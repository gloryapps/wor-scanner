package com.gloryapps.worscanner.scanner.game

import com.gloryapps.worscanner.scanner.text.ValueUnit
import com.gloryapps.worscanner.scanner.text.nameIn
import com.gloryapps.worscanner.scanner.text.numbersIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The eleven words an attribute goes by on a card, primary or secondary; the JSON carries the word. */
@Serializable
enum class Attribute(val word: String) {
    @SerialName("ATK") ATK("ATK"),
    @SerialName("DEF") DEF("DEF"),
    @SerialName("HP") HP("HP"),
    @SerialName("ATK Bonus") ATK_BONUS("ATK Bonus"),
    @SerialName("DEF Bonus") DEF_BONUS("DEF Bonus"),
    @SerialName("HP Bonus") HP_BONUS("HP Bonus"),
    @SerialName("Rage Regen") RAGE_REGEN("Rage Regen"),
    @SerialName("Crit. Rate") CRIT_RATE("Crit. Rate"),
    @SerialName("Crit. DMG") CRIT_DAMAGE("Crit. DMG"),
    @SerialName("Healing Effect") HEALING_EFFECT("Healing Effect"),
    @SerialName("ATK Spd.") ATK_SPEED("ATK Spd."),
}

/** One attribute as a card prints it: its word, the number beside it, and the green extra some cards add. */
@Serializable
data class ReadAttribute(val name: Attribute, val value: Double, val unit: ValueUnit, val bonus: Double?)

/** The attribute a row names behind its icon's noise. */
internal fun attributeIn(row: String): Attribute? = nameIn(row, Attribute.entries) { it.word }

/** Everything above the first row that names an attribute: a banner, a title, an exclusive's name. */
internal fun headOf(rows: List<String>): List<String> {
    val first = rows.indexOfFirst { attributeIn(it) != null }

    return if (first < 0) rows else rows.subList(0, first)
}

/**
 * The attribute rows in the order the panel prints them, each attribute once; the kind cuts the
 * rows where its own end.
 *
 * A value the recogniser set apart from its name lands on the row below, which is taken unless
 * that row names an attribute of its own. The first `extras` rows print a green extra beside their
 * value; on any other a second number is the recogniser's noise.
 */
internal fun attributesIn(rows: List<String>, extras: Int): List<ReadAttribute> {
    val held = mutableListOf<ReadAttribute>()

    for ((at, row) in rows.withIndex()) {
        val name = attributeIn(row) ?: continue
        if (held.any { it.name == name }) continue

        val below = rows.getOrNull(at + 1) ?: ""
        val own = numbersIn(row)
        val spilled = if (attributeIn(below) != null) emptyList() else numbersIn(below)
        val numbers = own.ifEmpty { spilled }
        val value = numbers.firstOrNull() ?: continue

        val bonus = if (held.size < extras) numbers.getOrNull(1)?.value else null
        held += ReadAttribute(name, value.value, value.unit, bonus)
    }

    return held
}
