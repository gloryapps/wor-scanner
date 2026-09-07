package com.gloryapps.worscanner.scanner.game

/** A name the wiki lists and the id the JSON carries for it. */
data class Named(val id: String, val name: String)

/** The ten factions a hero belongs to and a piece can be exclusive to as a whole. */
val FACTIONS: List<Named> = listOf(
    Named("chaos-dominion", "Chaos Dominion"),
    Named("cursed-cult", "Cursed Cult"),
    Named("esoteria-order", "Esoteria Order"),
    Named("infernal-blast", "Infernal Blast"),
    Named("nightmare-council", "Nightmare Council"),
    Named("north-throne", "North Throne"),
    Named("star-piercers", "Star Piercers"),
    Named("supreme-arbiter", "Supreme Arbiter"),
    Named("unnamable", "Unnamable"),
    Named("watchguard", "Watchguard"),
)
