package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.game.Attribute
import com.gloryapps.worscanner.scanner.game.ReadAttribute
import com.gloryapps.worscanner.scanner.kinds.artifact.ScannedArtifact
import com.gloryapps.worscanner.scanner.kinds.gear.ScannedGear
import com.gloryapps.worscanner.scanner.kinds.gear.Slot
import com.gloryapps.worscanner.scanner.kinds.hero.HeroSkills
import com.gloryapps.worscanner.scanner.kinds.hero.ScannedHero
import com.gloryapps.worscanner.scanner.kinds.hero.SkillLevel
import com.gloryapps.worscanner.scanner.text.ValueUnit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** The account's lists as the cards a scan of each kind writes, in `game`'s words; its gear and artifacts only those `choices` keeps. */
class Cards(private val game: Game, private val account: Account, private val choices: ScanChoices) {
    val gear: List<ScannedGear> by lazy {
        account.gear.filter { entry -> entry.int("iIntensifyLvl")?.let(Enhancement::of) in choices.enhancements }.mapNotNull(::piece)
    }

    /** The epic and legendary heroes, the furthest raised copy where the account holds two. */
    val heroes: List<ScannedHero> by lazy {
        account.heroes
            .filter { (it.long("iBaseId")?.let(game.heroes::get)?.rarity ?: 0) >= EPIC }
            .groupBy { it.long("iBaseId") }.values
            .map { copies -> copies.maxWith(compareBy({ it.int("iStarLevel") }, { it.int("iSublimLevel") }, { it.int("iLevel") })) }
            .mapNotNull(::hero)
    }

    val artifacts: List<ScannedArtifact> by lazy {
        account.artifacts.mapNotNull(::artifact).filter { choices.artifacts.keeps(it.level ?: 0, exclusive = it.exclusive != null) }
    }

    private fun piece(entry: JsonElement): ScannedGear? {
        val known = entry.long("iItemId")?.let(game.gear::get) ?: return null
        val main = entry.list("vMasterAttrList").firstOrNull()?.let { attribute(it.int("iAttrId"), it.long("iValue"), entry.long("iExtraMasterAttrValue")?.takeIf { bonus -> bonus != 0L }) }

        return ScannedGear(
            set = known.set,
            slot = Slot.entries.firstOrNull { it.word == known.slot },
            ancient = entry.int("iStarLvl")?.let { it in ANCIENT } == true,
            variant = entry.long("iVaryEffectId")?.let { game.variants[it shr EFFECT_SHIFT]?.get(it and EFFECT_INDEX) },
            exclusive = entry.long("iExclusiveEffectId")?.let { game.exclusives[it shr EFFECT_SHIFT] },
            attributes = (listOfNotNull(main) + entry.list("vViceAttrList").mapNotNull { attribute(it.int("iAttrId"), it.long("iValue"), null) }).distinctBy { it.name },
        )
    }

    /* A skill the account holds no level for is at 1, or at 0 while a promotion still locks it. */
    private fun hero(entry: JsonElement): ScannedHero? {
        val known = entry.long("iBaseId")?.let(game.heroes::get) ?: return null
        val levels = entry.levels()
        val promotion = entry.int("iSublimLevel") ?: 0

        return ScannedHero(
            name = known.name,
            level = entry.int("iLevel"),
            stars = entry.int("iStarLevel"),
            promotion = promotion,
            awakening = ((entry.long("iAwakeningFlag") ?: 0L) and AWAKENINGS).countOneBits(),
            skills = HeroSkills(
                ultimate = SkillLevel.Of(levels[0] ?: 1),
                row = known.row.map { skill -> SkillLevel.Of(skill.at?.let(levels::get) ?: if (skill.from > promotion) 0 else 1) },
            ),
        )
    }

    /* A level's own HP and ATK print with what refining added beside them; refining's other lines and the runic ones print alone. */
    private fun artifact(entry: JsonElement): ScannedArtifact? {
        val known = entry.long("iItemId")?.let(game.artifacts::get) ?: return null
        val level = entry.int("iLevel") ?: return null
        val own = game.artifactLevels[known.levels]?.getOrNull(level - 1).orEmpty()
        val refined = entry.list("vRefineAttrs").associate { it.int("uiAttrId") to it.long("uiValue") }
        val owned = OWN.mapIndexedNotNull { at, id -> attribute(id, own.getOrNull(at)?.toLong() ?: 0L, refined[id]) }
        val apart = (entry.list("vRefineAttrs").filter { it.int("uiAttrId")?.let { id -> id in OWN } != true } + entry.list("vDarkAttrs"))
            .mapNotNull { attribute(it.int("uiAttrId"), it.long("uiValue"), null) }

        return ScannedArtifact(
            name = known.name,
            level = level,
            skill = (entry.int("iStageLvl") ?: 0) + 1,
            exclusive = known.exclusive,
            attributes = owned + apart,
        )
    }

    private fun attribute(id: Int?, value: Long?, bonus: Long?): ReadAttribute? {
        val known = game.attributes[id ?: return null] ?: return null
        val name = Attribute.entries.firstOrNull { it.word == known.word } ?: return null

        return ReadAttribute(name, known.printed(value ?: return null), if (known.percentage) ValueUnit.PERCENTAGE else ValueUnit.FLAT, bonus?.let(known::printed))
    }

    private companion object {
        /** The client's star tiers with an ancient piece's numbers: 10 and 11, and 13, a variant of an ancient piece, whose card prints Variant instead. */
        val ANCIENT = setOf(10, 11, 13)
        const val EPIC = 4
        /** The attribute types of an artifact level's own HP and ATK, in the order `artifactLevels` keeps them. */
        val OWN = listOf(7, 1)
        /** A piece's exclusive or variant effect: its group in the upper half of the id the account keeps, its index in the lower. */
        const val EFFECT_SHIFT = 32
        const val EFFECT_INDEX = 0xFFFF_FFFFL
        /** The flags of Awakened I to V and beyond, one bit each. */
        const val AWAKENINGS = 0xFFL
    }
}

private fun GameAttribute.printed(kept: Long): Double = if (hundredths) kept / 100.0 else kept.toDouble()

private fun JsonElement.field(name: String): JsonElement? = (this as? JsonObject)?.get(name)

/** A number, which the file writes as a string where Lua kept it as one, as it does a big uid. */
private fun JsonElement.long(name: String): Long? = (field(name) as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() }

private fun JsonElement.int(name: String): Int? = long(name)?.toInt()

/** A Lua sequence as the file holds it: an array, or an object keyed from 1. */
private fun JsonElement.list(name: String): List<JsonElement> = when (val held = field(name)) {
    is JsonArray -> held
    is JsonObject -> held.entries.sortedBy { it.key.toIntOrNull() }.map { it.value }
    else -> emptyList()
}

/** A hero's skill levels by their place in the client's level-up list; the file keeps a table keyed from 1 as an array, the first element at 1. */
private fun JsonElement.levels(): Map<Int, Int> = when (val held = field("mSkillLevel")) {
    is JsonArray -> held.mapIndexedNotNull { at, level -> (level as? JsonPrimitive)?.intOrNull?.let { at + 1 to it } }.toMap()
    is JsonObject -> held.entries.mapNotNull { (at, level) -> at.toIntOrNull()?.let { place -> (level as? JsonPrimitive)?.intOrNull?.let { place to it } } }.toMap()
    else -> emptyMap()
}
