package com.gloryapps.worscanner.scanner.catalogue

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

/** The two halves the storage shows: weapons and armors left, accessories right. */
enum class Side { WEAPONS_BREASTPLATES, BANGLES_AMULETS_RINGS }

/** The five slots, each on its side; a Weapon always leads with ATK and a Breastplate with HP. */
@Serializable
enum class Slot(val word: String, val side: Side, val primary: Attribute?) {
    @SerialName("Weapon") WEAPON("Weapon", Side.WEAPONS_BREASTPLATES, Attribute.ATK),
    @SerialName("Breastplate") BREASTPLATE("Breastplate", Side.WEAPONS_BREASTPLATES, Attribute.HP),
    @SerialName("Bangle") BANGLE("Bangle", Side.BANGLES_AMULETS_RINGS, null),
    @SerialName("Amulet") AMULET("Amulet", Side.BANGLES_AMULETS_RINGS, null),
    @SerialName("Ring") RING("Ring", Side.BANGLES_AMULETS_RINGS, null),
}
