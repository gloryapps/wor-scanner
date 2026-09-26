package com.gloryapps.worscanner.scanner.kinds.hero

import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.countIn
import com.gloryapps.worscanner.scanner.scan.labelledTileAt
import com.gloryapps.worscanner.scanner.scan.topRowCentre
import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Line
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The hero screen as ML Kit read it inside LDPlayer at 1280x720, Ingrid selected on Attributes at the top of the grid. */
class LdPlayerHeroReadingTest {
    @Serializable
    private class Reading(val width: Int, val height: Int, val lines: List<Line>)

    private val reading = Json { ignoreUnknownKeys = true }
        .decodeFromString<Reading>(javaClass.getResource("/ldplayer-heroes-1280x720.json")!!.readText())

    private val frame = object : Frame {
        override val width = reading.width
        override val height = reading.height
        override fun colourAt(x: Int, y: Int) = Colour(0)
    }

    private val seen = Seen(frame, reading.lines)
    private val layout = HERO_ROSTER

    @Test
    fun `the header's count is read from its region, the icon read into it as a letter`() {
        assertEquals(212, countIn(seen.rowsIn(layout.count)))
    }

    @Test
    fun `the top row is the first row of tiles`() {
        val centre = topRowCentre(reading.lines, layout, frame)!!

        assertTrue(centre in 172..186, "row zero's centre read as $centre")
    }

    @Test
    fun `every column of the first row has a tile, though two of its labels read as one line`() {
        val top = topRowCentre(reading.lines, layout, frame)!!

        for (column in 0 until layout.columns) {
            assertTrue(labelledTileAt(reading.lines, layout, frame, column, top), "no tile at column $column")
        }
    }

    @Test
    fun `the panel region reads the selected hero's name and level`() {
        val hero = HeroScan.readScreen(seen)

        assertEquals("INGRID", hero.name)
        assertEquals(60, hero.level)
    }
}
