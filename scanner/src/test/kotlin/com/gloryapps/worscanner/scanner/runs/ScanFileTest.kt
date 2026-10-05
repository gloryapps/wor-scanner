package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanFileTest {
    private val entries = emptyList<ScanEntry<Int>>()

    @Test
    fun `an outcome goes on the wire as the word the lab parses, and comes back as how it ended`() {
        val wires = listOf(
            Outcome.Finished(entries),
            Outcome.Stopped(Outcome.Reason.GRID_LOST, entries, "lost"),
            Outcome.Failed(IllegalStateException("no frame"), entries),
        ).map { it.wire() }

        assertEquals(listOf("finished", "stopped:grid_lost", "failed:java.lang.IllegalStateException: no frame"), wires)
        assertEquals(listOf(Ended.FINISHED, Ended.STOPPED, Ended.FAILED), wires.map(::endedOf))
    }

    @Test
    fun `a word no scan writes ended no way`() {
        assertNull(endedOf("paused"))
    }

    @Test
    fun `the file names its version, which is what the lab checks first`() {
        val scan = ScanFile<Int>(kind = "gear", startedAt = "20260909-120000", width = 1280, height = 720, outcome = "finished", entries = emptyList())

        val text = Json.encodeToString(ScanFile.serializer(Int.serializer()), scan)

        assertTrue("\"version\":2" in text, text)
    }
}
