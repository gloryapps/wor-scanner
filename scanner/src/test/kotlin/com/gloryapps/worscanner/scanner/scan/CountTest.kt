package com.gloryapps.worscanner.scanner.scan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CountTest {
    @Test
    fun `the header's count is the pieces held`() {
        assertEquals(1169, countIn(listOf("1,169/2,500")))
    }

    @Test
    fun `the recogniser's spaces around the slash do not matter`() {
        assertEquals(37, countIn(listOf("Rarity", "37 / 2,500")))
    }

    @Test
    fun `no count, no storage`() {
        assertNull(countIn(listOf("STORAGE", "Gear")))
    }
}
