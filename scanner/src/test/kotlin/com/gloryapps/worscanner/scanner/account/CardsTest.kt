package com.gloryapps.worscanner.scanner.account

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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Entries as Pedro's account held them on 2026-10-10, and rules as the game's code lays them out, read in the shipped game's words. */
class CardsTest {
    private fun cards(
        heroes: List<String> = emptyList(),
        gear: List<String> = emptyList(),
        artifacts: List<String> = emptyList(),
        collected: List<Long> = emptyList(),
        rules: List<String> = emptyList(),
        choices: ScanChoices = ScanChoices.EVERYTHING,
    ) = Cards(
        Game.shipped,
        Account(startedAt = "20261010-122135", heroes = heroes.map(Json::parseToJsonElement), gear = gear.map(Json::parseToJsonElement),
            artifacts = artifacts.map(Json::parseToJsonElement), collected = collected, rules = rules.map(Json::parseToJsonElement), found = emptyMap(), seconds = 0.0),
        choices,
    )

    private fun percent(name: Attribute, value: Double) = ReadAttribute(name, value, ValueUnit.PERCENTAGE, null)

    private fun flat(name: Attribute, value: Double, bonus: Double? = null) = ReadAttribute(name, value, ValueUnit.FLAT, bonus)

    @Test
    fun `a piece read off the memory is the card the panel printed when it was scanned, ancient where its Variant banner hides it`() {
        assertEquals(
            listOf(
                ScannedGear(
                    set = "cataclysm",
                    slot = Slot.BANGLE,
                    ancient = true,
                    variant = "on-the-house",
                    variantEffect = "Increases DMG by 2%. No Gold is required when enhancing this gear.",
                    exclusive = "Vierna",
                    attributes = listOf(
                        percent(Attribute.ATK_BONUS, 66.0),
                        flat(Attribute.ATK, 469.0),
                        percent(Attribute.CRIT_RATE, 13.5),
                        percent(Attribute.HP_BONUS, 17.0),
                        percent(Attribute.DEF_BONUS, 25.5),
                    ),
                ),
            ),
            cards(gear = listOf(VIERNAS_BANGLE)).gear,
        )
    }

    @Test
    fun `a variant piece says which of its variant's effects it rolled, and the attributes that effect adds`() {
        val bloodthirst = VIERNAS_BANGLE.replace(""""iVaryEffectId":4312147165305""", """"iVaryEffectId":${(1001L shl 32) + 55}""")

        val card = cards(gear = listOf(bloodthirst)).gear.single()

        assertEquals("bloodthirst" to "ATK Spd. +20", card.variant to card.variantEffect)
        assertEquals(listOf(flat(Attribute.ATK_SPEED, 20.0)), card.variantAttributes)
    }

    @Test
    fun `only the pieces in the bands of enhancement chosen are cards`() {
        val unraised = VIERNAS_BANGLE.replace(""""iIntensifyLvl":16""", """"iIntensifyLvl":3""")

        assertEquals(1, cards(gear = listOf(VIERNAS_BANGLE, unraised), choices = ScanChoices(setOf(Enhancement.AT_16))).gear.size)
        assertEquals(0, cards(gear = listOf(unraised), choices = ScanChoices(setOf(Enhancement.AT_16))).gear.size)
        assertEquals(2, cards(gear = listOf(VIERNAS_BANGLE, unraised)).gear.size)
    }

    @Test
    fun `worth wearing keeps an artifact at +25, and an exclusive one from +10`() {
        val exclusiveAt10 = SPEAR_OF_LEONIDAS.replace(""""iLevel":25""", """"iLevel":10""")
        val exclusiveAt9 = SPEAR_OF_LEONIDAS.replace(""""iLevel":25""", """"iLevel":9""")
        val worthWearing = ScanChoices(artifacts = ArtifactsToScan.WORTH_WEARING)

        assertEquals(listOf(25, 10), cards(artifacts = listOf(SPEAR_OF_LEONIDAS, exclusiveAt10, exclusiveAt9, PLAIN_AT_24), choices = worthWearing).artifacts.map { it.level })
        assertEquals(4, cards(artifacts = listOf(SPEAR_OF_LEONIDAS, exclusiveAt10, exclusiveAt9, PLAIN_AT_24)).artifacts.size)
    }

    @Test
    fun `an artifact prints its level's own HP and ATK with what refining added, and its runic line apart`() {
        assertEquals(
            listOf(
                ScannedArtifact(
                    name = "Spear of Leonidas",
                    level = 25,
                    skill = 6,
                    exclusive = "Kassandra",
                    attributes = listOf(flat(Attribute.HP, 4650.0, bonus = 1730.0), flat(Attribute.ATK, 1497.0, bonus = 575.0), percent(Attribute.ATK_BONUS, 4.7)),
                ),
            ),
            cards(artifacts = listOf(SPEAR_OF_LEONIDAS)).artifacts,
        )
    }

    @Test
    fun `a hero's skills land where the lab's row puts them, one never raised at 1`() {
        assertEquals(
            listOf(
                ScannedHero(
                    name = "Pelagios",
                    level = 60,
                    stars = 6,
                    promotion = 6,
                    awakening = 0,
                    skills = HeroSkills(ultimate = SkillLevel.Of(3), row = listOf(SkillLevel.Of(3), SkillLevel.Of(2), SkillLevel.Of(2), SkillLevel.Of(1))),
                ),
            ),
            cards(heroes = listOf(PELAGIOS)).heroes,
        )
    }

    @Test
    fun `a skill a promotion still locks is at 0`() {
        val unpromoted = PELAGIOS.replace(""""iSublimLevel":6""", """"iSublimLevel":0""").replace("""{"1":2,"2":2,"3":3,"0":3}""", """{"0":1}""")

        assertEquals(listOf(SkillLevel.Of(1), SkillLevel.Of(0), SkillLevel.Of(0), SkillLevel.Of(1)), cards(heroes = listOf(unpromoted)).heroes.single().skills.row)
    }

    @Test
    fun `awakenings are the flags lit`() {
        assertEquals(listOf(2), cards(heroes = listOf(PELAGIOS.replace(""""iAwakeningFlag":0""", """"iAwakeningFlag":5"""))).heroes.map { it.awakening })
    }

    @Test
    fun `of two copies of a hero the further raised is the card`() {
        val second = PELAGIOS.replace("222400000", "222400001").replace(""""iStarLevel":6""", """"iStarLevel":5""")

        assertEquals(listOf(6), cards(heroes = listOf(second, PELAGIOS)).heroes.map { it.stars })
    }

    @Test
    fun `a hero below epic is no card`() {
        assertEquals(emptyList(), cards(heroes = listOf(PELAGIOS.replace(""""iBaseId":2224""", """"iBaseId":${Game.shipped.heroes.entries.first { it.value.rarity < 4 }.key}"""))).heroes)
    }

    @Test
    fun `the collection counts each hero the account has held once, under each of its factions, whatever its rarity`() {
        val (rex, brienne, nunea) = listOf(2001L, 2002L, 2003L)

        val collection = cards(collected = listOf(rex, brienne, brienne, nunea)).collection

        assertEquals(listOf(2, 1, 0), listOf("The Watchguard", "The Star Piercers", "The Unnamable").map(collection::getValue))
        assertEquals(FACTIONS, collection.keys)
    }

    @Test
    fun `a hero the shipped game does not know counts under no faction`() {
        assertEquals(setOf(0), cards(collected = listOf(1L)).collection.values.toSet())
    }

    @Test
    fun `a rule of the plan reads in the game's words, its sets in the order the game lists them and each secondary essential or in the pool`() {
        assertEquals(
            listOf(
                ScannedRule(
                    name = "Crit DPS",
                    side = Side.BANGLES_AMULETS_RINGS,
                    sets = listOf("fatality", "the-doom"),
                    main = listOf(Attribute.CRIT_DAMAGE),
                    secondaries = mapOf(Attribute.ATK to Demand.POOL, Attribute.ATK_BONUS to Demand.POOL, Attribute.CRIT_RATE to Demand.ESSENTIAL),
                    match = 2,
                ),
            ),
            cards(rules = listOf(CRIT_DPS)).rules,
        )
    }

    @Test
    fun `an attribute the game keeps that is none of the eleven words is not one of the rule's main attributes`() {
        val widened = CRIT_DPS.replace(""""MainAttrMap":[25]""", """"MainAttrMap":[25,37,38,40]""")

        assertEquals(listOf(Attribute.CRIT_DAMAGE), cards(rules = listOf(widened)).rules.single().main)
    }

    @Test
    fun `a set the game names in no English, or the shipped game does not hold, stays in its rule as none`() {
        val unnamed = CRIT_DPS.replace(""""SuitMap":[721300,720400]""", """"SuitMap":[999900,400005,721300]""")

        assertEquals(listOf("the-doom", null, null), cards(rules = listOf(unnamed)).rules.single().sets)
    }

    @Test
    fun `the weapons and breastplates side's rules come first, each side's in the order the game lists them`() {
        val second = CRIT_DPS.replace("Crit DPS", "Crit DPS 2")

        assertEquals(listOf("Tanks", "Crit DPS", "Crit DPS 2"), cards(rules = listOf(CRIT_DPS, TANKS, second)).rules.map { it.name })
    }

    @Test
    fun `a rule on neither side is no card`() {
        assertEquals(emptyList(), cards(rules = listOf(CRIT_DPS.replace(""""SuitType":2""", """"SuitType":0"""))).rules)
    }

    private companion object {
        val FACTIONS = setOf(
            "The Watchguard", "The North Throne", "The Nightmare Council", "The Cursed Cult", "The Infernal Blast", "The Star Piercers",
            "The Esoteria Order", "The Chaos Dominion", "The Supreme Arbiters", "The Grey Blades", "The Unnamable",
        )

        const val VIERNAS_BANGLE = """{"iItemUid":27420,"iItemId":3218803,"iStarLvl":13,"iIntensifyLvl":16,"vMasterAttrList":[{"iId":304,"iAttrId":13,"iValue":6600}],""" +
            """"vViceAttrList":[{"iId":303,"iAttrId":1,"iValue":469},{"iId":309,"iAttrId":24,"iValue":1350},{"iId":302,"iAttrId":19,"iValue":1700},{"iId":306,"iAttrId":14,"iValue":2550}],""" +
            """"iExtraMasterAttrValue":0,"iExclusiveEffectId":38684770435543,"iVaryEffectId":4312147165305,"iHeroId":0,"bLocked":false}"""
        const val SPEAR_OF_LEONIDAS = """{"iItemUid":"10058813626433","iItemId":219201,"iLevel":25,"iStageLvl":5,"iHeroId":235500001,""" +
            """"vRefineAttrs":[{"uiAttrId":1,"uiValue":575},{"uiAttrId":7,"uiValue":1730}],"vDarkAttrs":[{"uiAttrId":13,"uiValue":470}]}"""
        const val PLAIN_AT_24 = """{"iItemUid":"4295169997","iItemId":202701,"iLevel":24,"iStageLvl":4,"iHeroId":0}"""
        const val CRIT_DPS = """{"ID":3,"Name":"Crit DPS","IsSystem":false,"SuitType":2,"SuitMap":[721300,720400],"MainAttrMap":[25],""" +
            """"SubAttrMap":[1,13,24],"MustAttrMap":[24],"SubAttrSatisfied":2,"iLastUpdateTime":1760090000}"""
        const val TANKS = """{"ID":4,"Name":"Tanks","IsSystem":false,"SuitType":1,"SuitMap":[711400],"MainAttrMap":[],""" +
            """"SubAttrMap":[19,14,7],"MustAttrMap":[],"SubAttrSatisfied":3,"iLastUpdateTime":1760090000}"""
        const val PELAGIOS = """{"iHeroId":222400000,"iBaseId":2224,"iLevel":60,"iStarLevel":6,"iSublimLevel":6,"iAwakeningFlag":0,"mSkillLevel":{"1":2,"2":2,"3":3,"0":3}}"""
    }
}
