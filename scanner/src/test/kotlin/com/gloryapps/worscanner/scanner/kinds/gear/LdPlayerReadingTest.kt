package com.gloryapps.worscanner.scanner.kinds.gear

import com.gloryapps.worscanner.scanner.scan.countIn
import com.gloryapps.worscanner.scanner.scan.labelledTileAt
import com.gloryapps.worscanner.scanner.scan.recorded
import com.gloryapps.worscanner.scanner.scan.topRowCentre
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The storage as ML Kit read it inside LDPlayer at 1280x720, with the first piece selected. */
class LdPlayerReadingTest {
    private val seen = recorded("ldplayer-storage-1280x720.json")
    private val layout = GEAR_STORAGE

    @Test
    fun `the header's count is read from its region`() {
        assertEquals(1169, countIn(seen.rowsIn(layout.count)))
    }

    @Test
    fun `the top row is the first row of tiles, not the second`() {
        val centre = topRowCentre(seen.lines, layout, seen.frame)!!

        assertTrue(centre in 195..215, "row zero's centre read as $centre")
    }

    @Test
    fun `every column of the first three rows has a tile`() {
        val top = topRowCentre(seen.lines, layout, seen.frame)!!
        val pitch = layout.pitchY(seen.frame.height)

        for (row in 0..2) {
            for (column in 0 until layout.columns) {
                assertTrue(labelledTileAt(seen.lines, layout, seen.frame, column, top + row * pitch), "no tile at row $row column $column")
            }
        }
    }

    @Test
    fun `the whole frame reads the selected piece as the recording kept it`() {
        val kept = Json.parseToJsonElement(javaClass.getResource("/ldplayer-storage-1280x720.json")!!.readText()).jsonObject.getValue("card")

        assertEquals(Json.decodeFromJsonElement(ScannedGear.serializer(), kept), GearScan.readScreen(seen))
    }
}
