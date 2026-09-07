package com.gloryapps.worscanner.scanner.gear

/** A set as the wiki's Gear page names it; the id is what the JSON carries. */
data class GearSet(val id: String, val name: String, val tier: Int, val side: Side)

private fun left(id: String, name: String, tier: Int) = GearSet(id, name, tier, Side.WEAPONS_BREASTPLATES)
private fun right(id: String, name: String, tier: Int) = GearSet(id, name, tier, Side.BANGLES_AMULETS_RINGS)

/** Transcribed from the wiki's Gear page, both tables, spelled as the page spells them. */
val GEAR_SETS: List<GearSet> = listOf(
    left("drakefire", "Drakefire", 3),
    left("goldmane", "Goldmane", 3),
    left("astral-guardian", "Astral Guardian", 3),
    left("lights-grace", "Light's Grace", 3),
    left("wicked-vengeance", "Wicked Vengeance", 3),
    left("immortal-warrior", "Immortal Warrior", 2),
    left("warlord", "Warlord", 2),
    left("salvation", "Salvation", 1),
    left("life-force", "Life Force", 1),
    left("calamity", "Calamity", 1),
    left("whirlwind", "Whirlwind", 1),
    left("annihilating-might", "Annihilating Might", 1),
    left("lifegiver", "Lifegiver", 0),
    left("iron-fortress", "Iron Fortress", 0),
    left("wrathful-onslaught", "Wrathful Onslaught", 0),
    left("savage-strike", "Savage Strike", 0),
    left("deadly-aim", "Deadly Aim", 0),
    left("vitality", "Vitality", 0),
    left("juggernaut", "Juggernaut", 0),
    right("greyfang", "Greyfang", 3),
    right("wings-of-grace", "Wings of Grace", 3),
    right("cataclysm", "Cataclysm", 3),
    right("hells-lament", "Hell's Lament", 3),
    right("tempered-will", "Tempered Will", 3),
    right("unshaken-will", "Unshaken Will", 3),
    right("morale", "Morale", 3),
    right("infernal-roar", "Infernal Roar", 2),
    right("soulbound-arcana", "Soulbound Arcana", 2),
    right("ageless-wrath", "Ageless Wrath", 2),
    right("undying-savage", "Undying Savage", 2),
    right("invigoration", "Invigoration", 2),
    right("asclepius", "Asclepius", 1),
    right("the-insight", "The Insight", 1),
    right("the-wisdom", "The Wisdom", 1),
    right("the-glacier", "The Glacier", 1),
    right("night-terror", "Night Terror", 1),
    right("fracture", "Fracture", 1),
    right("curse", "Curse", 1),
    right("the-doom", "The Doom", 1),
    right("hawk-eye", "Hawk Eye", 1),
    right("mana-spring", "Mana Spring", 1),
    right("the-styx", "The Styx", 1),
    right("fatality", "Fatality", 1),
    right("guardian", "Guardian", 1),
    right("occult-shield", "Occult Shield", 0),
    right("twisted-blade", "Twisted Blade", 0),
    right("rapidity", "Rapidity", 0),
    right("the-tempest", "The Tempest", 0),
)

fun gearSetOf(id: String): GearSet? = GEAR_SETS.find { it.id == id }
