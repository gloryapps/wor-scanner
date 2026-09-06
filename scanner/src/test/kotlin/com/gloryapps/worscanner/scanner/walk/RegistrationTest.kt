package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.reading.Box
import com.gloryapps.worscanner.scanner.reading.Line
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegistrationTest {
    private val grid = Box(100, 200, 700, 850)
    private fun word(text: String, x: Int, y: Int) = Line(text, Box(x, y, x + 40, y + 16))

    @Test
    fun `the shift every true pair agrees on wins over the pairs that repeat a word`() {
        val before = listOf(word("+16", 120, 500), word("1056", 120, 540), word("+16", 250, 500), word("66%", 250, 540), word("+16", 120, 700), word("1056", 120, 740))
        val after = before.map { it.copy(box = Box(it.box.left, it.box.top - 187, it.box.right, it.box.bottom - 187)) }

        assertEquals(187, shiftByText(before, after, grid, 80))
    }

    @Test
    fun `a frame with none of the words is no shift at all`() {
        val before = listOf(word("1056", 120, 340), word("66%", 250, 340), word("3960", 120, 540))
        val after = listOf(word("STORAGE", 120, 340), word("Rarity", 250, 340))

        assertNull(shiftByText(before, after, grid, 80))
    }

    @Test
    fun `words outside the grid do not vote`() {
        val before = listOf(word("1056", 120, 340), word("66%", 250, 340), word("3960", 120, 540), word("1,169/2,500", 600, 150))
        val after = listOf(word("1056", 120, 240), word("66%", 250, 240), word("3960", 120, 440), word("1,169/2,500", 600, 150))

        assertEquals(100, shiftByText(before, after, grid, 80))
    }
}
