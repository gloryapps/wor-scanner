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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Entries as Pedro's account held them on 2026-10-10, read in the shipped game's words. */
class CardsTest {
    private fun cards(heroes: List<String> = emptyList(), gear: List<String> = emptyList(), artifacts: List<String> = emptyList()) = Cards(
        Game.shipped,
        Account(startedAt = "20261010-122135", heroes = heroes.map(Json::parseToJsonElement), gear = gear.map(Json::parseToJsonElement),
            artifacts = artifacts.map(Json::parseToJsonElement), found = emptyMap(), seconds = 0.0),
    )

    private fun percent(name: Attribute, value: Double) = ReadAttribute(name, value, ValueUnit.PERCENTAGE, null)

    private fun flat(name: Attribute, value: Double, bonus: Double? = null) = ReadAttribute(name, value, ValueUnit.FLAT, bonus)

    @Test
    fun `a piece read off the memory is the card the panel printed when it was scanned`() {
        assertEquals(
            listOf(
                ScannedGear(
                    set = "cataclysm",
                    slot = Slot.BANGLE,
                    ancient = false,
                    variant = true,
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

    private companion object {
        const val VIERNAS_BANGLE = """{"iItemUid":27420,"iItemId":3218803,"iStarLvl":13,"iIntensifyLvl":16,"vMasterAttrList":[{"iId":304,"iAttrId":13,"iValue":6600}],""" +
            """"vViceAttrList":[{"iId":303,"iAttrId":1,"iValue":469},{"iId":309,"iAttrId":24,"iValue":1350},{"iId":302,"iAttrId":19,"iValue":1700},{"iId":306,"iAttrId":14,"iValue":2550}],""" +
            """"iExtraMasterAttrValue":0,"iExclusiveEffectId":38684770435543,"iVaryEffectId":4312147165305,"iHeroId":0,"bLocked":false}"""
        const val SPEAR_OF_LEONIDAS = """{"iItemUid":"10058813626433","iItemId":219201,"iLevel":25,"iStageLvl":5,"iHeroId":235500001,""" +
            """"vRefineAttrs":[{"uiAttrId":1,"uiValue":575},{"uiAttrId":7,"uiValue":1730}],"vDarkAttrs":[{"uiAttrId":13,"uiValue":470}]}"""
        const val PELAGIOS = """{"iHeroId":222400000,"iBaseId":2224,"iLevel":60,"iStarLevel":6,"iSublimLevel":6,"iAwakeningFlag":0,"mSkillLevel":{"1":2,"2":2,"3":3,"0":3}}"""
    }
}
