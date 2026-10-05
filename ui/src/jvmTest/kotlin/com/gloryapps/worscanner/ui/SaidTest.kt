package com.gloryapps.worscanner.ui

import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.runs.Kept
import com.gloryapps.worscanner.scanner.runs.ReadScreen
import com.gloryapps.worscanner.scanner.runs.ScanState
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.Progress
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SaidTest {
    @Test
    fun `a Read kept says its stamp and how many lines it read`() = runBlocking {
        val read = Result.success(ReadScreen.Read(Kept.Read("20261005-120000", emptyList()), lines = 42))

        assertEquals("Kept 20261005-120000, 42 lines", read.said())
    }

    @Test
    fun `a Read that failed says why`() = runBlocking {
        val read = Result.failure<ReadScreen.Read>(IllegalStateException("Watcher of Realms is not open"))

        assertEquals("Reading failed: Watcher of Realms is not open", read.said())
    }

    private val read = List(12) { ScanEntry(it, 0, it, card = it, rows = emptyList()) }

    @Test
    fun `no scan begun says nothing`() = runBlocking {
        assertNull(ScanState.Idle.said())
    }

    @Test
    fun `a scan under way says how far it is`() = runBlocking {
        assertEquals("Scanning Gear: 12 read, 1169 held", ScanState.Running(Kind.GEAR, Progress(12, 1169)).said())
    }

    @Test
    fun `a scan's end says how it ended and how much it read`() = runBlocking {
        val ends = listOf(
            Outcome.Finished(read),
            Outcome.Stopped(Outcome.Reason.CANCELLED, read, "the mouse moved"),
            Outcome.Failed(IllegalStateException("no frame"), read),
        ).map { ScanState.Ended(Kind.HEROES, it, "20261005-120000").said() }

        assertEquals(
            listOf(
                "Heroes scan finished: 12 read",
                "Heroes scan stopped: the mouse moved. 12 kept.",
                "Heroes scan failed: java.lang.IllegalStateException: no frame",
            ),
            ends,
        )
    }
}
