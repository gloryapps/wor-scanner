package com.gloryapps.worscanner.scanner.text

import com.gloryapps.worscanner.scanner.game.Attribute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatchingTest {
    @Test
    fun `a name is found behind the icon's noise`() {
        assertTrue(holdsName("Jx Cataclysm", "Cataclysm"))
    }

    @Test
    fun `a slip of one character still lands`() {
        assertTrue(holdsName("Cataclysn", "Cataclysm"))
    }

    @Test
    fun `a short word takes no slack`() {
        assertFalse(holdsName("A7K 1056", "ATK"))
    }

    @Test
    fun `ATK does not shadow ATK Bonus`() {
        assertEquals(Attribute.ATK_BONUS, nameIn("ATK Bonus 66%", Attribute.entries) { it.word })
    }
}
