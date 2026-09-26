package com.gloryapps.worscanner.scanner.kinds.gear

/** A set as the wiki's Gear page names it; the id is what the JSON carries. */
data class GearSet(val id: String, val name: String, val side: Side)

private fun left(id: String, name: String) = GearSet(id, name, Side.WEAPONS_BREASTPLATES)
private fun right(id: String, name: String) = GearSet(id, name, Side.BANGLES_AMULETS_RINGS)

/** Transcribed from the wiki's Gear page, both tables, spelled as the page spells them. */
val GEAR_SETS: List<GearSet> = listOf(
    left("drakefire", "Drakefire"),
    left("goldmane", "Goldmane"),
    left("astral-guardian", "Astral Guardian"),
    left("lights-grace", "Light's Grace"),
    left("wicked-vengeance", "Wicked Vengeance"),
    left("immortal-warrior", "Immortal Warrior"),
    left("warlord", "Warlord"),
    left("salvation", "Salvation"),
    left("life-force", "Life Force"),
    left("calamity", "Calamity"),
    left("whirlwind", "Whirlwind"),
    left("annihilating-might", "Annihilating Might"),
    left("lifegiver", "Lifegiver"),
    left("iron-fortress", "Iron Fortress"),
    left("wrathful-onslaught", "Wrathful Onslaught"),
    left("savage-strike", "Savage Strike"),
    left("deadly-aim", "Deadly Aim"),
    left("vitality", "Vitality"),
    left("juggernaut", "Juggernaut"),
    right("greyfang", "Greyfang"),
    right("wings-of-grace", "Wings of Grace"),
    right("cataclysm", "Cataclysm"),
    right("hells-lament", "Hell's Lament"),
    right("tempered-will", "Tempered Will"),
    right("unshaken-will", "Unshaken Will"),
    right("morale", "Morale"),
    right("infernal-roar", "Infernal Roar"),
    right("soulbound-arcana", "Soulbound Arcana"),
    right("ageless-wrath", "Ageless Wrath"),
    right("undying-savage", "Undying Savage"),
    right("invigoration", "Invigoration"),
    right("asclepius", "Asclepius"),
    right("the-insight", "The Insight"),
    right("the-wisdom", "The Wisdom"),
    right("the-glacier", "The Glacier"),
    right("night-terror", "Night Terror"),
    right("fracture", "Fracture"),
    right("curse", "Curse"),
    right("the-doom", "The Doom"),
    right("hawk-eye", "Hawk Eye"),
    right("mana-spring", "Mana Spring"),
    right("the-styx", "The Styx"),
    right("fatality", "Fatality"),
    right("guardian", "Guardian"),
    right("occult-shield", "Occult Shield"),
    right("twisted-blade", "Twisted Blade"),
    right("rapidity", "Rapidity"),
    right("the-tempest", "The Tempest"),
)

fun gearSetOf(id: String): GearSet? = GEAR_SETS.find { it.id == id }
