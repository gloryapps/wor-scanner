package com.gloryapps.worscanner.scanner.text

import kotlin.test.Test
import kotlin.test.assertEquals

class NumbersTest {
    @Test
    fun `a row's numbers are the run it ends on, each with its unit`() {
        assertEquals(listOf(Amount(3960.0, ValueUnit.FLAT), Amount(400.0, ValueUnit.FLAT)), numbersIn("HP 3960 +400"))
        assertEquals(listOf(Amount(13.5, ValueUnit.PERCENTAGE)), numbersIn("Crit. Rate 13,5%"))
    }

    @Test
    fun `an O between digits is read as the zero it is`() {
        assertEquals(listOf(Amount(4000.0, ValueUnit.FLAT), Amount(1050.0, ValueUnit.FLAT)), numbersIn("HP 4000+1O50"))
    }

    @Test
    fun `letters glued to the last number are left off it`() {
        assertEquals(listOf(Amount(1497.0, ValueUnit.FLAT), Amount(830.0, ValueUnit.FLAT)), numbersIn("ATK 1497+830o"))
    }

    @Test
    fun `a number inside the name is not a value`() {
        assertEquals(emptyList(), numbersIn("A7K Bonus"))
    }
}
