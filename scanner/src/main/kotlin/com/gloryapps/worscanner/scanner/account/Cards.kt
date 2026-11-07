package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.client.Client
import com.gloryapps.worscanner.scanner.client.unitName
import com.gloryapps.worscanner.scanner.game.Attribute
import com.gloryapps.worscanner.scanner.game.ReadAttribute
import com.gloryapps.worscanner.scanner.kinds.artifact.ScannedArtifact
import com.gloryapps.worscanner.scanner.kinds.gear.ScannedGear
import com.gloryapps.worscanner.scanner.kinds.gear.Side
import com.gloryapps.worscanner.scanner.kinds.gear.Slot
import com.gloryapps.worscanner.scanner.kinds.hero.HeroSkills
import com.gloryapps.worscanner.scanner.kinds.hero.ScannedHero
import com.gloryapps.worscanner.scanner.kinds.hero.SkillLevel
import com.gloryapps.worscanner.scanner.text.ValueUnit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** The account's lists as the cards a scan of each kind writes, its heroes' collection and its plan's rules, in `game`'s words and its heroes in `client`'s; its gear and artifacts only those `choices` keeps. */
class Cards(private val game: Game, private val account: Account, private val choices: ScanChoices, private val client: Client? = null) {
    val gear: List<ScannedGear> by lazy {
        account.gear.filter { entry -> entry.int("iIntensifyLvl")?.let(Enhancement::of) in choices.enhancements }.mapNotNull(::piece)
    }

    /** The epic and legendary heroes, the furthest raised copy where the account holds two. */
    val heroes: List<ScannedHero> by lazy {
        account.heroes
            .filter { (it.long("iBaseId")?.let(roster::get)?.rarity ?: 0) >= EPIC }
            .groupBy { it.long("iBaseId") }.values
            .map { copies -> copies.maxWith(compareBy({ it.int("iStarLevel") }, { it.int("iSublimLevel") }, { it.int("iLevel") })) }
            .mapNotNull(::hero)
    }

    /** How many heroes of each faction the account has collected, as the game's Collection Progress counts them: of any rarity, one of two factions in both. */
    val collection: Map<String, Int> by lazy {
        /* A hero who rises in rarity is a unit a form, each printed by his one name, and the game counts him once. */
        val collected = account.collected.distinct().mapNotNull(roster::get).distinctBy { it.name }

        roster.values.flatMap { it.factions }.distinct().associateWith { faction -> collected.count { faction in it.factions } }
    }

    /** Every hero the client holds, by base id. */
    private val roster: Map<Long, GameHero> by lazy {
        client?.let { client -> client.units().mapNotNull { id -> client.hero(id)?.let { id to it } }.toMap() }.orEmpty()
    }

    val artifacts: List<ScannedArtifact> by lazy {
        account.artifacts.mapNotNull(::artifact).filter { choices.artifacts.keeps(it.level ?: 0, exclusive = it.exclusive != null) }
    }

    /** The squads of every squad screen open in the game, each hero with the pieces and the artifact he wears, whatever `choices` keeps. */
    val squads: List<Squads> by lazy {
        val bases = account.heroes.mapNotNull { hero -> hero.long(HERO_ID)?.let { it to hero.long("iBaseId") } }.toMap()

        account.squads.mapNotNull { screen ->
            Squads(
                stageId = screen.long(SQUAD_STAGE) ?: return@mapNotNull null,
                squads = screen.list(SQUAD_TEAMS).map { squad ->
                    squad.sequence().mapNotNull { slot ->
                        val id = slot.long(SLOT_HERO)?.takeIf { it != EMPTY_SLOT } ?: return@mapNotNull null
                        val base = bases[id]
                        SquadHero(
                            heroId = id,
                            baseId = base,
                            name = base?.let { client?.unitName(it) },
                            lord = (slot.field(SLOT_LORD) as? JsonPrimitive)?.let { it.longOrNull != null || it.booleanOrNull == true } == true,
                            pieces = account.gear.filter { it.long(HERO_ID) == id }.mapNotNull(::piece).sortedBy { it.slot?.ordinal },
                            artifact = account.artifacts.firstOrNull { it.long(HERO_ID) == id }?.let(::artifact),
                        )
                    }
                },
            )
        }
    }

    /** The weapons and breastplates side's first, each side's in the order the game lists them. */
    val rules: List<ScannedRule> by lazy { account.rules.mapNotNull(::rule).sortedBy { it.side } }

    private fun piece(entry: JsonElement): ScannedGear? {
        val known = entry.long("iItemId")?.let(game.gear::get) ?: return null
        val variant = entry.long("iVaryEffectId")?.let { game.variants[it shr EFFECT_SHIFT]?.get(it and EFFECT_INDEX) }
        val main = entry.list("vMasterAttrList").firstOrNull()?.let { attribute(it.int("iAttrId"), it.long("iValue"), entry.long("iExtraMasterAttrValue")?.takeIf { bonus -> bonus != 0L }) }

        return ScannedGear(
            set = known.set,
            slot = Slot.entries.firstOrNull { it.word == known.slot },
            ancient = entry.int("iStarLvl")?.let { it in ANCIENT } == true,
            variant = variant?.slug,
            variantEffect = variant?.effect,
            variantAttributes = variant?.attributes.orEmpty().mapNotNull { (id, value) -> attribute(id.toInt(), value, null) },
            exclusive = entry.long("iExclusiveEffectId")?.let { game.exclusives[it shr EFFECT_SHIFT] },
            attributes = (listOfNotNull(main) + entry.list("vViceAttrList").mapNotNull { attribute(it.int("iAttrId"), it.long("iValue"), null) }).distinctBy { it.name },
        )
    }

    /* A skill the account holds no level for is at 1, or at 0 while a promotion still locks it. */
    private fun hero(entry: JsonElement): ScannedHero? {
        val known = entry.long("iBaseId")?.let(roster::get) ?: return null
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

    /* A rule given no main attribute holds, once the game reads it, every main its sets' pieces roll: the same pieces pass. */
    private fun rule(entry: JsonElement): ScannedRule? {
        val essential = entry.ids("MustAttrMap").mapNotNull(::named).toSet()

        return ScannedRule(
            name = entry.text("Name").orEmpty(),
            side = entry.int("SuitType")?.let(SIDES::get) ?: return null,
            sets = entry.ids("SuitMap").map { game.sets[it] }.sortedWith(compareBy(nullsLast()) { it?.sort }).map { it?.set },
            main = entry.ids("MainAttrMap").mapNotNull(::named).sorted(),
            secondaries = (entry.ids("SubAttrMap").mapNotNull(::named) + essential).sorted().associateWith { if (it in essential) Demand.ESSENTIAL else Demand.POOL },
            match = entry.int("SubAttrSatisfied") ?: return null,
        )
    }

    private fun named(id: Long): Attribute? = game.attributes[id.toInt()]?.named

    private fun attribute(id: Int?, value: Long?, bonus: Long?): ReadAttribute? {
        val known = game.attributes[id ?: return null] ?: return null
        val name = known.named ?: return null

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
        /** The side a rule's sets are worn on, by the number the game keeps it as. */
        val SIDES = mapOf(1 to Side.WEAPONS_BREASTPLATES, 2 to Side.BANGLES_AMULETS_RINGS)
    }
}

/** A hero's own id, which the pieces and artifacts he wears hold too, 0 for those he does not. */
private const val HERO_ID = "iHeroId"

/** A unit's type where the client makes it a hero. */
private const val HERO = 1
/** The promotion unlock that adds a skill to a hero's row, by the number the game keeps its kind as. */
private const val UNLOCKS_SKILL = 1L

/** A hero's name, rarity (4 epic, 5 legendary), the skills after the ultimate in the lab's order, and the factions it counts under, as the game prints them. */
private data class GameHero(val name: String, val rarity: Int, val row: List<GameSkill>, val factions: List<String>)

/** Where a skill's level sits in the account's levels, none where it is never raised, and the promotion that unlocks it. */
private data class GameSkill(val at: Int?, val from: Int)

/* A unit is a hero where the client types it one and it raises skills: the first it leaves unraised is its basic attack, the ones promotions unlock follow, the first it raises is its ultimate. */
private fun Client.hero(id: Long): GameHero? {
    val row = unit(id)?.takeIf { it.int("m_Type") == HERO } ?: return null
    val raised = row.numbers("m_HeroSkillLevelupList").takeIf { it.isNotEmpty() } ?: return null
    val basic = row.numbers("m_HeroSkillNotLevelupList").firstOrNull()?.let { GameSkill(at = raised.indexOf(it).takeIf { at -> at >= 0 }, from = 0) }
    val unlocked = row.text("m_AdvancedUnlock").orEmpty().split(';').mapNotNull { unlock ->
        val (from, kind, skill) = unlock.split(',').mapNotNull { it.trim().toLongOrNull() }.takeIf { it.size >= 3 } ?: return@mapNotNull null
        if (kind != UNLOCKS_SKILL || skill !in raised || skill == raised.first()) null else GameSkill(at = raised.indexOf(skill), from = from.toInt())
    }

    return GameHero(
        name = unitName(id) ?: return null,
        rarity = row.int("m_Quality") ?: return null,
        row = listOfNotNull(basic) + unlocked.sortedBy { it.at },
        factions = row.numbers("m_OnShowFactionMember").mapNotNull { text("HeroFaction_mFactionName_$it") },
    )
}

private fun GameAttribute.printed(kept: Long): Double = if (hundredths) kept / 100.0 else kept.toDouble()

private val GameAttribute.named: Attribute? get() = Attribute.entries.firstOrNull { it.word == word }

private fun JsonElement.field(name: String): JsonElement? = (this as? JsonObject)?.get(name)

/** A number, which the file writes as a string where Lua kept it as one, as it does a big uid. */
private fun JsonElement.long(name: String): Long? = (field(name) as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() }

private fun JsonElement.int(name: String): Int? = long(name)?.toInt()

private fun JsonElement.text(name: String): String? = (field(name) as? JsonPrimitive)?.takeIf { it.isString }?.content

/** The numbers a Lua sequence holds, in its order. */
private fun JsonElement.numbers(name: String): List<Long> = list(name).mapNotNull { (it as? JsonPrimitive)?.longOrNull }

/** The ids a field keyed by them was read as. */
private fun JsonElement.ids(name: String): List<Long> = (field(name) as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.longOrNull }.orEmpty()

private fun JsonElement.list(name: String): List<JsonElement> = field(name)?.sequence().orEmpty()

/** A Lua sequence as the file holds it: an array, or an object keyed from 1. */
private fun JsonElement.sequence(): List<JsonElement> = when (this) {
    is JsonArray -> this
    is JsonObject -> entries.sortedBy { it.key.toIntOrNull() }.map { it.value }
    else -> emptyList()
}

/** A hero's skill levels by their place in the client's level-up list; the file keeps a table keyed from 1 as an array, the first element at 1. */
private fun JsonElement.levels(): Map<Int, Int> = when (val held = field("mSkillLevel")) {
    is JsonArray -> held.mapIndexedNotNull { at, level -> (level as? JsonPrimitive)?.intOrNull?.let { at + 1 to it } }.toMap()
    is JsonObject -> held.entries.mapNotNull { (at, level) -> at.toIntOrNull()?.let { place -> (level as? JsonPrimitive)?.intOrNull?.let { place to it } } }.toMap()
    else -> emptyMap()
}
