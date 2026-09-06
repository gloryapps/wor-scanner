package com.gloryapps.worscanner.scanner.catalogue

/** The eleven words an attribute goes by on a card, primary or secondary. */
enum class Attribute(val word: String) {
    ATK("ATK"),
    DEF("DEF"),
    HP("HP"),
    ATK_BONUS("ATK Bonus"),
    DEF_BONUS("DEF Bonus"),
    HP_BONUS("HP Bonus"),
    RAGE_REGEN("Rage Regen"),
    CRIT_RATE("Crit. Rate"),
    CRIT_DAMAGE("Crit. DMG"),
    HEALING_EFFECT("Healing Effect"),
    ATK_SPEED("ATK Spd."),
}

/** The two halves the storage shows: weapons and armors left, accessories right. */
enum class Side { WEAPONS_BREASTPLATES, BANGLES_AMULETS_RINGS }

/** The five slots, each on its side; a Weapon always leads with ATK and a Breastplate with HP. */
enum class Slot(val word: String, val side: Side, val primary: Attribute?) {
    WEAPON("Weapon", Side.WEAPONS_BREASTPLATES, Attribute.ATK),
    BREASTPLATE("Breastplate", Side.WEAPONS_BREASTPLATES, Attribute.HP),
    BANGLE("Bangle", Side.BANGLES_AMULETS_RINGS, null),
    AMULET("Amulet", Side.BANGLES_AMULETS_RINGS, null),
    RING("Ring", Side.BANGLES_AMULETS_RINGS, null),
}
