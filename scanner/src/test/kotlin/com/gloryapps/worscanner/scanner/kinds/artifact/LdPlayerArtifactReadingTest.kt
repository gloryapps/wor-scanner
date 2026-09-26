package com.gloryapps.worscanner.scanner.kinds.artifact

import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.scan.countIn
import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Line
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Storage's Artifact tab as ML Kit read it inside LDPlayer at 1280x720, Spear of Leonidas selected at the top of the grid. */
class LdPlayerArtifactReadingTest {
    @Serializable
    private class Reading(val width: Int, val height: Int, val lines: List<Line>)

    private val reading = Json { ignoreUnknownKeys = true }
        .decodeFromString<Reading>(javaClass.getResource("/ldplayer-artifacts-1280x720.json")!!.readText())

    private val frame = object : Frame {
        override val width = reading.width
        override val height = reading.height
        override fun colourAt(x: Int, y: Int) = Colour(0)
    }

    private val seen = Seen(frame, reading.lines)
    private val layout = ARTIFACT_STORAGE

    @Test
    fun `the header's count is read from its region`() {
        assertEquals(423, countIn(seen.rowsIn(layout.count)))
    }

    @Test
    fun `the panel region reads the selected artifact, the overlay's sheet above it left out`() {
        val artifact = ArtifactScan.readScreen(seen)

        assertEquals("Spear of Leonidas", artifact.name)
        assertEquals(25, artifact.level)
        assertEquals(6, artifact.skill)
        assertEquals(3, artifact.attributes.size)
    }
}
