package com.gloryapps.worscanner.scanner.senses

import kotlin.test.Test
import kotlin.test.assertEquals

class ColourTest {
    /** A promoted star's left half, as LDPlayer's display packs it. */
    private val purple = Colour(0xFF854DFF.toInt())

    @Test
    fun `a colour reads its three channels off the packed pixel, the alpha left aside`() {
        assertEquals(Triple(133, 77, 255), Triple(purple.red, purple.green, purple.blue))
    }

    @Test
    fun `a colour's paleness is its least channel`() {
        assertEquals(77, purple.paleness)
        assertEquals(200, Colour(0xFFC8CBF5.toInt()).paleness)
    }

    @Test
    fun `a colour's hue puts a promoted star near violet and an unpromoted one near yellow`() {
        assertEquals(258.9, purple.hue, 0.1)
        assertEquals(52.1, Colour(0xFFF6E67D.toInt()).hue, 0.1)
    }

    @Test
    fun `a legendary's frame and an epic's tell apart by hue`() {
        assertEquals(45.5, Colour(0xFFC5AA55.toInt()).hue, 0.1)
        assertEquals(272.9, Colour(0xFF4B236C.toInt()).hue, 0.1)
    }

    @Test
    fun `an empty star's grey has neither saturation nor hue, where a promoted one is saturated`() {
        val grey = Colour(0xFF7D7D7D.toInt())

        assertEquals(0.0, grey.saturation)
        assertEquals(0.0, grey.hue)
        assertEquals(0.70, purple.saturation, 0.01)
    }
}
