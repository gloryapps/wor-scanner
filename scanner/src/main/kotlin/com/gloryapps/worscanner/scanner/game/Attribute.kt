package com.gloryapps.worscanner.scanner.game

import com.gloryapps.worscanner.scanner.text.ValueUnit
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
