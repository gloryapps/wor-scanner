package com.gloryapps.worscanner.scanner.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordsTest {
    private val slots = listOf("Weapon", "Breastplate", "Bangle", "Amulet", "Ring")

    @Test
    fun `a word a character off still names its candidate`() {
        assertEquals("Amulet", wordIn("Infernal Roar Amnulet", slots) { it })
        assertEquals("Bangle", wordIn("Guardian Banglet", slots) { it })
    }

    @Test
    fun `a short word takes no slack`() {
        assertNull(wordIn("Cataclysm King", slots) { it })
    }
}
