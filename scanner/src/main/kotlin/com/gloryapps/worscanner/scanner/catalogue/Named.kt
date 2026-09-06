package com.gloryapps.worscanner.scanner.catalogue

/** A name the page lists and the id the JSON carries for it. */
data class Named(val id: String, val name: String)

/** The variants a piece can carry on top of its set, transcribed typos and all. */
val GEAR_VARIANTS: List<Named> = listOf(
    Named("agression", "Agression"),
    Named("benediction", "Benediction"),
    Named("blood-sacrifice", "Blood Sacrifice"),
    Named("bloodthirst", "Bloodthirst"),
    Named("brutality", "Brutality"),
    Named("bulwark", "Bulwark"),
    Named("divine-fury", "Divine Fury"),
    Named("fortitude", "Fortitude"),
    Named("grace", "Grace"),
    Named("incindiary-roar", "Incindiary Roar"),
    Named("languid-curse", "Languid Curse"),
    Named("last-stand", "Last Stand"),
    Named("merciless", "Merciless"),
    Named("on-the-house", "On the House"),
    Named("rapidity", "Rapidity"),
    Named("recklessness", "Recklessness"),
    Named("renewed-vigor", "Renewed Vigor"),
    Named("retaliation", "Retaliation"),
    Named("spirit-mastery", "Spirit Mastery"),
    Named("steady-poise", "Steady Poise"),
    Named("stoicism", "Stoicism"),
    Named("tital-wave", "Tital Wave"),
    Named("the-immovable-object", "The Immovable Object"),
    Named("the-unstoppable-force", "The Unstoppable Force"),
    Named("unrivalled-power", "Unrivalled Power"),
    Named("well-nourished", "Well-Nourished"),
    Named("wisdom", "Wisdom"),
    Named("witches-charm", "Witches' Charm"),
    Named("woeful-optimism", "Woeful Optimism"),
)

/** The ten factions a piece can be exclusive to as a whole. */
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
