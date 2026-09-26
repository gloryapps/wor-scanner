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

    @Test
    fun `a word read with slips is counted each time it sits in the line`() {
        assertEquals(3, timesHeld("Max Level Max Letel Max Level", "Max Level"))
        assertEquals(1, timesHeld("1/5Mextevel", "Max Level"))
        assertEquals(2, timesHeld("Max Level Max Eevel ", "Max Level"))
    }

    @Test
    fun `a line that holds no such word counts none`() {
        assertEquals(0, timesHeld("Random Upgrade", "Max Level"))
        assertEquals(0, timesHeld("Lvl. 2/5", "Max Level"))
    }
}
