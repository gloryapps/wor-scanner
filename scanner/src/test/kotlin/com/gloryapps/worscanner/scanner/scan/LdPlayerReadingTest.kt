package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.gear.GEAR_STORAGE
import com.gloryapps.worscanner.scanner.gear.GearReader
import com.gloryapps.worscanner.scanner.kind.holds
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Line
import com.gloryapps.worscanner.scanner.text.rowsOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The storage as ML Kit read it inside LDPlayer at 1280x720, with the first piece selected. */
class LdPlayerReadingTest {
    @Serializable
    private class Reading(val width: Int, val height: Int, val lines: List<Line>)

    private val reading = Json { ignoreUnknownKeys = true }
        .decodeFromString<Reading>(javaClass.getResource("/ldplayer-storage-1280x720.json")!!.readText())

    private val frame = object : Frame {
        override val width = reading.width
        override val height = reading.height
        override fun palenessAt(x: Int, y: Int) = 0
    }

    private val layout = GEAR_STORAGE

    @Test
    fun `the header's count is read from its region`() {
        val box = layout.count.box(frame.width, frame.height)

        assertEquals(1169, countIn(rowsOf(reading.lines.filter { box.holds(it) })))
    }

    @Test
    fun `the top row is the first row of tiles, not the second`() {
        val centre = topRowCentre(reading.lines, layout, frame)!!

        assertTrue(centre in 195..215, "row zero's centre read as $centre")
    }

    @Test
    fun `every column of the first three rows has a tile`() {
        val top = topRowCentre(reading.lines, layout, frame)!!
        val pitch = layout.pitchY(frame.height)

        for (row in 0..2) {
            for (column in 0 until layout.columns) {
                assertTrue(tileAt(reading.lines, layout, frame, column, top + row * pitch), "no tile at row $row column $column")
            }
        }
    }

    @Test
    fun `the panel region reads the selected piece whole`() {
        val box = layout.panel.box(frame.width, frame.height)
        val card = GearReader.read(rowsOf(reading.lines.filter { box.holds(it) }))

        assertEquals("cataclysm", card.set)
        assertEquals("VIERNA", card.exclusive)
        assertEquals(5, card.attributes.size)
    }
}
