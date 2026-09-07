package com.gloryapps.worscanner.scanner.kinds.gear

import com.gloryapps.worscanner.scanner.game.Attribute
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
