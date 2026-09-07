package com.gloryapps.worscanner.scanner.text

import kotlin.test.Test
import kotlin.test.assertEquals

class RowsTest {
    private fun line(text: String, left: Int, top: Int) = Line(text, Box(left, top, left + 10 * text.length, top + 20))

    @Test
    fun `a name and a value at the same height are one row, left to right`() {
        val rows = rowsOf(listOf(line("66%", 440, 100), line("ATK Bonus", 190, 102)))

        assertEquals(listOf("ATK Bonus 66%"), rows)
    }

    @Test
    fun `lines at different heights are rows in reading order`() {
        val rows = rowsOf(listOf(line("Exclusive", 250, 150), line("VIERNA", 250, 110)))

        assertEquals(listOf("VIERNA", "Exclusive"), rows)
    }
}
