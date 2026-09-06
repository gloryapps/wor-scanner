package com.gloryapps.worscanner.scanner.catalogue

import kotlin.test.Test
import kotlin.test.assertEquals

class SetsTest {
    @Test
    fun `every set has an id of its own`() {
        assertEquals(GEAR_SETS.size, GEAR_SETS.map { it.id }.toSet().size)
    }

    @Test
    fun `the page lists nineteen sets on the left and twenty-nine on the right`() {
        assertEquals(19, GEAR_SETS.count { it.side == Side.WEAPONS_BREASTPLATES })
        assertEquals(29, GEAR_SETS.count { it.side == Side.BANGLES_AMULETS_RINGS })
    }
}
