package com.gloryapps.worscanner.scanner.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameTextsTest {
    @Test
    fun `a key hashes as the client hashes Clavicus's name and his faction's`() {
        assertEquals(listOf(0x21F7C994L, 0x8CD20EECL).map(Long::toInt), listOf("Unit_mName_2903", "HeroFaction_mFactionName_103").map(::hash))
    }

    @Test
    fun `a text is read whole by its key`() {
        val texts = GameTexts(textTable(mapOf("Unit_mDesc_2903" to "é".repeat(100), "Unit_mName_2903" to "Clavicus")))

        assertEquals(listOf("é".repeat(100), "Clavicus"), listOf("Unit_mDesc_2903", "Unit_mName_2903").map(texts::text))
    }

    @Test
    fun `a text the English leaves blank is read from the extra texts`() {
        val texts = GameTexts(textTable(mapOf("Unit_mName_2903" to "")), textTable(mapOf("Unit_mName_2903" to "Clavicus")))

        assertEquals("Clavicus", texts.text("Unit_mName_2903"))
    }

    @Test
    fun `a key neither table holds has no text`() {
        assertNull(GameTexts(textTable(mapOf("Unit_mName_2001" to "Rex")), textTable(emptyMap())).text("Unit_mName_2903"))
    }
}
