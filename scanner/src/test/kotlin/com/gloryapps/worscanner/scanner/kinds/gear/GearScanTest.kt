package com.gloryapps.worscanner.scanner.kinds.gear

import com.gloryapps.worscanner.scanner.game.Attribute
import com.gloryapps.worscanner.scanner.game.ReadAttribute
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import com.gloryapps.worscanner.scanner.text.ValueUnit
import com.gloryapps.worscanner.scanner.text.rowsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GearScanTest {
    private fun at(text: String, left: Int, top: Int, right: Int, bottom: Int) = Line(text, Box(left, top, right, bottom))

    /** The panel of the storage screenshot of 2026-09-06: Vierna's bangle, placed as the game draws it. */
    private val viernasBangle = listOf(
        at("Variant Mythic Gear", 1265, 205, 1460, 222),
        at("Variant: Vierna's Bangle", 1265, 230, 1470, 248),
        at("VIERNA", 1250, 310, 1330, 328),
        at("Exclusive", 1250, 345, 1370, 368),
        at("ATK Bonus", 1190, 395, 1335, 420),
        at("66%", 1440, 395, 1485, 420),
        at("ATK", 1190, 455, 1230, 470),
        at("469", 1450, 455, 1485, 470),
        at("Crit. Rate", 1190, 508, 1280, 526),
        at("13.5%", 1440, 508, 1485, 526),
        at("HP Bonus", 1190, 565, 1285, 582),
        at("17%", 1450, 565, 1485, 582),
        at("DEF Bonus", 1190, 620, 1295, 638),
        at("25.5%", 1440, 620, 1485, 638),
        at("Cataclysm", 1165, 705, 1275, 725),
        at("(3 pieces) Upon landing a Crit.", 1140, 740, 1460, 760),
    )

    @Test
    fun `an exclusive variant bangle reads whole`() {
        val card = GearScan.read(rowsOf(viernasBangle))

        assertEquals("cataclysm", card.set)
        assertEquals(Slot.BANGLE, card.slot)
        assertEquals(false, card.ancient)
        assertEquals(null, card.variant)
        assertEquals("VIERNA", card.exclusive)
        assertEquals(
            listOf(
                ReadAttribute(Attribute.ATK_BONUS, 66.0, ValueUnit.PERCENTAGE, null),
                ReadAttribute(Attribute.ATK, 469.0, ValueUnit.FLAT, null),
                ReadAttribute(Attribute.CRIT_RATE, 13.5, ValueUnit.PERCENTAGE, null),
                ReadAttribute(Attribute.HP_BONUS, 17.0, ValueUnit.PERCENTAGE, null),
                ReadAttribute(Attribute.DEF_BONUS, 25.5, ValueUnit.PERCENTAGE, null),
            ),
            card.attributes,
        )
    }

    @Test
    fun `a piece is called by its title, the row that names its slot`() {
        assertEquals("Variant: Vierna's Bangle", GearScan.titleOf(rowsOf(viernasBangle)))
    }

    @Test
    fun `an exclusive whose hero did not read names no one, not the title above it`() {
        val card = GearScan.read(rowsOf(viernasBangle.filterNot { it.text == "VIERNA" }))

        assertNull(card.exclusive)
        assertEquals(Slot.BANGLE, card.slot)
    }

    @Test
    fun `a weapon whose ATK line lost its name takes the number back`() {
        val card = GearScan.read(
            listOf("Mythic Gear", "Whirlwind Weapon", "A7K 1056", "Crit. Rate 12%", "HP 2400", "Whirlwind", "(2 pieces) ATK Spd. +75"),
        )

        assertEquals("whirlwind", card.set)
        assertEquals(Slot.WEAPON, card.slot)
        assertEquals(
            listOf(
                ReadAttribute(Attribute.ATK, 1056.0, ValueUnit.FLAT, null),
                ReadAttribute(Attribute.CRIT_RATE, 12.0, ValueUnit.PERCENTAGE, null),
                ReadAttribute(Attribute.HP, 2400.0, ValueUnit.FLAT, null),
            ),
            card.attributes,
        )
    }

    @Test
    fun `the set block's attribute is not a sixth row`() {
        val card = GearScan.read(listOf("Cataclysm Ring", "ATK Bonus 60%", "Cataclysm", "(3 pieces) Basic ATK DMG +40%"))

        assertEquals(listOf(ReadAttribute(Attribute.ATK_BONUS, 60.0, ValueUnit.PERCENTAGE, null)), card.attributes)
    }

    @Test
    fun `a value set apart from its name is taken from the row below`() {
        val card = GearScan.read(listOf("Cataclysm Ring", "Crit. DMG", "44%", "DEF 291", "Cataclysm", "(3 pieces) Upon"))

        assertEquals(
            listOf(
                ReadAttribute(Attribute.CRIT_DAMAGE, 44.0, ValueUnit.PERCENTAGE, null),
                ReadAttribute(Attribute.DEF, 291.0, ValueUnit.FLAT, null),
            ),
            card.attributes,
        )
    }

    @Test
    fun `the first row's second number is its green extra`() {
        val card = GearScan.read(listOf("Ancient Mythic Gear", "Calamity Breastplate", "HP 3960 +400", "Calamity", "(2 pieces) ATK +25%"))

        assertEquals(true, card.ancient)
        assertEquals(Slot.BREASTPLATE, card.slot)
        assertEquals(listOf(ReadAttribute(Attribute.HP, 3960.0, ValueUnit.FLAT, 400.0)), card.attributes)
    }

    @Test
    fun `a lost banner on an accessory leaves the slot to the reader`() {
        val card = GearScan.read(listOf("JA deme", "Crit. Rate 30%", "Cataclysm", "(3 pieces) Upon"))

        assertEquals("cataclysm", card.set)
        assertNull(card.slot)
    }

    @Test
    fun `a set the catalogue lacks reads as nothing`() {
        val card = GearScan.read(listOf("Moonfall Ring", "ATK Bonus 60%", "Moonfall", "(3 pieces) Something"))

        assertNull(card.set)
        assertEquals(Slot.RING, card.slot)
    }

    /** Rows as the scan of 2026-09-07 kept them, one per line. */
    private fun rows(vararg text: String) = text.mapIndexed { index, row -> at(row, 1140, 200 + index * 40, 1480, 220 + index * 40) }

    @Test
    fun `Bonus read as Bornus is still the attribute`() {
        val card = GearScan.read(rowsOf(rows("+16 Ancient Mythic Gear", "Ancient: Infernal Roar", "Ring", "Crit. DMG 72%", "ATK Bornus 17.5%", "Crit. Rate 22%", "ATK Spd. 68", "ATK 419", "Infernal Roar", "(3 pieces) Basic ATK DMG +40%")))

        assertEquals(listOf(Attribute.CRIT_DAMAGE, Attribute.ATK_BONUS, Attribute.CRIT_RATE, Attribute.ATK_SPEED, Attribute.ATK), card.attributes.map { it.name })
    }

    @Test
    fun `a slot word a character off still names the slot`() {
        val amulet = GearScan.read(rowsOf(rows("+16 Mythic Gear", "Infernal Roar Amnulet", "HP Bonus 66%", "HP 2175", "Crit. Rate 23.5%", "Crit. DMG 31.5%", "ATK Spd. 67", "Infernal Roar", "(3 pieces) Basic ATK DMG +40%")))
        val bangle = GearScan.read(rowsOf(rows("+16 Mythic Gear", "Guardian Banglet", "HP Bonus 60%", "ATK 411", "HP 2420", "DEF Bonus 16.5%", "4 ATK Spd. 49", "Guardian", "(3 pieces) DMG Taken -15%")))

        assertEquals(Slot.AMULET, amulet.slot)
        assertEquals(Slot.BANGLE, bangle.slot)
    }

    @Test
    fun `a pieces count read as a letter still closes the card`() {
        val card = GearScan.read(rowsOf(rows("+16 Mythic Gear", "Undying Savage Ring", "Crit. DMG 72%", "Crit. Rate 24%", "Rage Regen 21.5%", "ATK Spd. 70", "HP Bonus 22.5%", "Undying Savage", "(B pieces) When the hero gains a", "shield or receives healing,", "increases Max HP by 1% and")))

        assertEquals("undying-savage", card.set)
        assertEquals(5, card.attributes.size)
    }

    @Test
    fun `a card is closed once its set and its slot are named`() {
        val closed = GearScan.read(listOf("Cataclysm Ring", "ATK Bonus 60%", "Cataclysm", "(3 pieces) Basic ATK DMG +40%"))
        val noSet = GearScan.read(listOf("Moonfall Ring", "ATK Bonus 60%", "Moonfall", "(3 pieces) Something"))
        val noSlot = GearScan.read(listOf("JA deme", "Crit. Rate 30%", "Cataclysm", "(3 pieces) Upon"))

        assertTrue(GearScan.closed(closed))
        assertFalse(GearScan.closed(noSet))
        assertFalse(GearScan.closed(noSlot))
    }
}
