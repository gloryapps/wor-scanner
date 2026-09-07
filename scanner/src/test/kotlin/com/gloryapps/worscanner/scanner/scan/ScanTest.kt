package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.senses.Frame
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanTest {
    private val kept = mutableListOf<Int>()
    private val keeper = Keeper { _: Frame, index -> kept += index; "$index.png" }

    private suspend fun Scan<Int>.run() = run(mutableListOf())

    private fun scanOver(storage: FakeStorage) = Scan(storage, storage, storage, storage, keeper, settleMillis = 0)

    /** The piece each entry read, which the fake prints on its panel. */
    private fun Outcome<Int>.pieces() = entries.map { it.card }

    @Test
    fun `every piece on one screen is tapped once, in reading order`() = runTest {
        val storage = FakeStorage(pieces = 10)

        val outcome = scanOver(storage).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 10).toList(), outcome.pieces())
        assertEquals((0 until 10).toList(), outcome.entries.map { it.index })
        assertEquals(10, storage.taps.size)
        assertEquals(0, storage.drags)
    }

    @Test
    fun `each entry holds the panel of the piece it tapped`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 30)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        outcome.entries.forEachIndexed { index, entry -> assertEquals(listOf("Piece $index"), entry.rows) }
        assertEquals((0 until 30).toList(), outcome.pieces())
    }

    @Test
    fun `the grid is dragged when the visible rows are done and the scan goes on where it landed`() = runTest {
        val storage = FakeStorage(pieces = 30)

        val outcome = scanOver(storage).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
        assertTrue(storage.drags >= 1)
    }

    @Test
    fun `a drag that stops between two rows is found out by the tiles, not assumed`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 35, rowsPerDrag = 1.4)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 35).toList(), outcome.pieces())
    }

    @Test
    fun `a drag that overshoots is found out just the same`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, rowsPerDrag = 1.6)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `the last rows settle on the viewport's floor and are still read, the partial one included`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 29)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 29).toList(), outcome.pieces())
    }

    @Test
    fun `the scan begins on the piece the game has selected`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 30, selected = 9)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((9 until 30).toList(), outcome.pieces())
        assertEquals(0, outcome.entries.first().index)
        assertEquals(0 to 2, outcome.entries.first().row to outcome.entries.first().column)
    }

    @Test
    fun `a grid left scrolled with nothing selected begins on the first whole row in view`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, scrolledRows = 1.4)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((14 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `a selected piece on a scrolled grid is where the scan begins`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, scrolledRows = 2.0, selected = 18)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((18 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `a row whose numbers come back as one line still has every tile`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 30, runsNumbersTogether = true)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
    }

    @Test
    fun `without the header's count the storage is not open`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 5, storageOpen = false)).run()

        assertIs<Outcome.Stopped<Int>>(outcome)
        assertEquals(Outcome.Reason.STORAGE_NOT_OPEN, outcome.reason)
    }

    @Test
    fun `a panel the reader cannot close is kept as an image and the scan goes on`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 5, unreadable = setOf(2))).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals(listOf(2), kept)
        assertEquals("2.png", outcome.entries[2].png)
        assertEquals(5, outcome.entries.size)
    }

    @Test
    fun `cancelling ends the scan by its exception and leaves the caller what was read so far`() = runTest {
        val storage = FakeStorage(pieces = 30)
        val entries = mutableListOf<ScanEntry<Int>>()
        var ended: Outcome<Int>? = null

        coroutineScope {
            val job = launch {
                ended = Scan(storage, storage, storage, storage, keeper, settleMillis = 1_000).run(entries)
            }
            testScheduler.advanceTimeBy(3_500)
            job.cancel()
            job.join()
        }

        assertNull(ended)
        assertEquals(3, entries.size)
    }

    @Test
    fun `a number the recogniser missed mid-row is still a tile, since only the last row runs short`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 30, unlabelled = setOf(4, 13, 28))).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
    }

    @Test
    fun `a last row short of a number is not made longer than it is`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 31, unlabelled = setOf(30))).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
    }

    @Test
    fun `a grid that keeps gliding after the drag is registered once it stops`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, rowsPerDrag = 0.4, glideRows = 0.6)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `tiles that all print the same number are registered by the framed tile instead`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, sameNumbers = true)).run()

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 42).toList(), outcome.pieces())
    }
}
