package com.gloryapps.worscanner.windows.ocr

import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import kotlin.test.Test
import kotlin.test.assertEquals

class LinesTest {
    private fun word(text: String, left: Int, top: Int = 100) = Word(text, Box(left, top, left + 10 * text.length, top + 16))

    @Test
    fun `a tile's word and a panel row on one baseline stay two lines`() {
        val lines = linesOf(listOf(listOf(word("Gear", 120), word("ATK", 900), word("Bonus", 935))))

        assertEquals(listOf("Gear", "ATK Bonus"), lines.map { it.text })
    }

    @Test
    fun `a row's name and its value far apart come back as two lines, as ML Kit gives them`() {
        val lines = linesOf(listOf(listOf(word("HP", 800), word("4650", 1100))))

        assertEquals(listOf("HP", "4650"), lines.map { it.text })
    }

    @Test
    fun `words a space apart are one line, in the box they make together`() {
        val lines = linesOf(listOf(listOf(word("Spear", 800, top = 102), word("of", 856), word("Leonidas", 882))))

        assertEquals(listOf(Line("Spear of Leonidas", Box(800, 100, 962, 118))), lines)
    }

    @Test
    fun `words given out of order are joined left to right`() {
        val lines = linesOf(listOf(listOf(word("Bonus", 935), word("ATK", 900))))

        assertEquals(listOf("ATK Bonus"), lines.map { it.text })
    }
}
