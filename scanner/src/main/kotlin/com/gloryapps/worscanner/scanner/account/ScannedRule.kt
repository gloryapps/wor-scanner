package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.game.Attribute
import com.gloryapps.worscanner.scanner.kinds.gear.Side
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The JSON's `kind` for a scan of the game's plan, which only an account read off the game's memory makes. */
const val PLAN = "plan"

/** One rule of the game's plan, as its settings list it: the pieces it marks as recommended. */
@Serializable
data class ScannedRule(
    val name: String,
    val side: Side,
    /** Its sets in the order the game lists them, as the lab's gear sets name them; null for one the game names in no English. */
    val sets: List<String?>,
    /** The main attributes it marks a piece for, any one of them; one given none lists every main its sets' pieces roll. */
    val main: List<Attribute>,
    val secondaries: Map<Attribute, Demand>,
    /** How many of its secondaries a piece must carry. */
    val match: Int,
)

/** What a rule asks of a secondary: one of the pool a piece may carry, or essential, on every piece it marks. */
@Serializable
enum class Demand {
    @SerialName("pool") POOL,
    @SerialName("essential") ESSENTIAL,
}
