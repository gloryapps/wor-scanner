package com.gloryapps.worscanner.scanner.game

import com.gloryapps.worscanner.scanner.text.ValueUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributeTest {
    /** Spear of Leonidas's rows, as the artifact tab printed them on 2026-09-26. */
    private val spear = listOf("Mythic Artifact", "Spear of Leonidas", "KASSANIDR", "Exclusive", "+ 25/25t", "HP 4650+1730", "ATK 1497+575", "ATK Bonus +4.7%")

    @Test
    fun `the first rows print a green extra beside their value`() {
        assertEquals(
            listOf(
                ReadAttribute(Attribute.HP, 4650.0, ValueUnit.FLAT, 1730.0),
                ReadAttribute(Attribute.ATK, 1497.0, ValueUnit.FLAT, 575.0),
                ReadAttribute(Attribute.ATK_BONUS, 4.7, ValueUnit.PERCENTAGE, null),
            ),
            attributesIn(spear, extras = 2),
        )
    }

    @Test
    fun `a second number past the rows with an extra is noise`() {
        val read = attributesIn(listOf("ATK 1056+50", "Crit. Rate 12% 4"), extras = 1)

        assertEquals(listOf(50.0, null), read.map { it.bonus })
    }

    @Test
    fun `a value set apart from its name is taken off the row below`() {
        assertEquals(listOf(ReadAttribute(Attribute.HP, 2200.0, ValueUnit.FLAT, null)), attributesIn(listOf("HP", "2200"), extras = 0))
    }

    @Test
    fun `a row below that names its own attribute keeps its value`() {
        val read = attributesIn(listOf("HP", "ATK 777"), extras = 0)

        assertEquals(listOf(ReadAttribute(Attribute.ATK, 777.0, ValueUnit.FLAT, null)), read)
    }

    @Test
    fun `an attribute named twice is read once`() {
        assertEquals(listOf(2200.0), attributesIn(listOf("HP 2200", "HP 3960"), extras = 0).map { it.value })
    }

    @Test
    fun `the head is every row above the first attribute`() {
        assertEquals(spear.take(5), headOf(spear))
    }
}
