package com.gloryapps.worscanner.scanner.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExclusiveTest {
    @Test
    fun `an exclusive is the name above the word, as read`() {
        assertEquals("KASSANIDR", exclusiveIn(listOf("Mythic Artifact", "Spear of Leonidas", "KASSANIDR", "Exclusive", "+ 25/25", "HP 4650+1730")))
    }

    @Test
    fun `the portrait's noise around the name is left out`() {
        assertEquals("EZEBELI", exclusiveIn(listOf("Mythic Artifact", "Perdition", "[EZEBELI.", "Exclusive", "+22/22", "HP 4300+1990")))
    }

    @Test
    fun `a panel without the word names no exclusive`() {
        assertNull(exclusiveIn(listOf("Mythic Artifact", "Hate's Contagion", "Class-Limited", "+ 25/25", "HP 4650+1790")))
    }

    @Test
    fun `the word below the attributes names no exclusive`() {
        assertNull(exclusiveIn(listOf("Mythic Artifact", "Helm of Helios", "HP 3100", "Exclusive")))
    }
}
