package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.senses.Touch
import com.gloryapps.worscanner.scanner.text.Box
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanTest {
    private val panels = mutableListOf<Int>()
    private val journal = mutableListOf<ScanEntry<Int>>()
    private val keeper = object : Keeper<Int> {
        override suspend fun panel(frames: List<Frame>, index: Int): String {
            panels += index

            return "$index.png"
        }

        override suspend fun entry(entry: ScanEntry<Int>) {
            journal += entry
        }
    }

    private suspend fun scanOver(
        storage: FakeStorage,
        scan: Scan<Int> = PieceScan(storage.layout),
        entries: MutableList<ScanEntry<Int>> = mutableListOf(),
        settleMillis: Long = 0,
        progress: suspend (Progress) -> Unit = {},
    ) = scan.run(storage, storage, storage, keeper, entries, settleMillis, progress)

    /** Every tab's panel for the piece, in the order the tabs sit. */
    private fun ScanEntry<Int>.underEveryTab(tabs: Int) = (0 until tabs).map { tab -> "Piece $card under tab $tab" }

    /** The piece each entry read, which the fake prints on its panel. */
    private fun Outcome<Int>.pieces() = entries.map { it.card }

    @Test
    fun `every piece on one screen is tapped once, in reading order`() = runTest {
        val storage = FakeStorage(pieces = 10)

        val outcome = scanOver(storage)

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 10).toList(), outcome.pieces())
        assertEquals((0 until 10).toList(), outcome.entries.map { it.index })
        assertEquals(10, storage.taps.size)
        assertEquals(0, storage.drags)
    }

    @Test
    fun `each entry holds the panel of the piece it tapped`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 30))

        assertIs<Outcome.Finished<Int>>(outcome)
        outcome.entries.forEachIndexed { index, entry -> assertEquals(listOf("Piece $index"), entry.rows) }
        assertEquals((0 until 30).toList(), outcome.pieces())
    }

    @Test
    fun `the grid is dragged when the visible rows are done and the scan goes on where it landed`() = runTest {
        val storage = FakeStorage(pieces = 30)

        val outcome = scanOver(storage)

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
        assertTrue(storage.drags >= 1)
    }

    @Test
    fun `a screen showing the grid another way is walked where that view puts its tiles`() = runTest {
        val squares = FAKE_GRID.copy(columns = 5, tilePitchX = 0.11, tilePitchY = 0.15)
        val storage = FakeStorage(pieces = 30, layout = squares)

        val outcome = scanOver(storage, ViewedPieceScan(squares))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
        assertTrue(storage.drags >= 1)
    }

    @Test
    fun `a piece the kind skips is tapped and not kept, and the progress still counts it`() = runTest {
        val storage = FakeStorage(pieces = 30)
        val progress = mutableListOf<Progress>()

        val outcome = scanOver(storage, PieceScan(storage.layout, skipped = setOf(3, 17))) { progress += it }

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30) - setOf(3, 17), outcome.pieces())
        assertEquals(outcome.entries, journal)
        assertEquals(30, storage.taps.size)
        assertEquals(Progress(30, 30), progress.last())
        assertEquals(0 to 4, outcome.entries.single { it.card == 4 }.let { it.row to it.column })
    }

    @Test
    fun `a drag is read once, on the frame the grid settled on, while the framed tile stays in view`() = runTest {
        val storage = FakeStorage(pieces = 30)

        val outcome = scanOver(storage)

        assertIs<Outcome.Finished<Int>>(outcome)
        assertTrue(storage.drags >= 1)
        assertEquals(1 + storage.taps.size + storage.drags, storage.reads)
    }

    @Test
    fun `a drag that stops between two rows is found out by the tiles, not assumed`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 35, rowsPerDrag = 1.4))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 35).toList(), outcome.pieces())
    }

    @Test
    fun `a drag that overshoots is found out just the same`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, rowsPerDrag = 1.6))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `a drag the game dropped mid-grid is tried again, not taken for the grid's end`() = runTest {
        val storage = FakeStorage(pieces = 42, droppedDrags = setOf(1, 3))

        val outcome = scanOver(storage)

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `the last rows settle on the viewport's floor and are still read, the partial one included`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 29))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 29).toList(), outcome.pieces())
    }

    @Test
    fun `the scan begins on the piece the game has selected`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 30, selected = 9))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((9 until 30).toList(), outcome.pieces())
        assertEquals(0, outcome.entries.first().index)
        assertEquals(0 to 2, outcome.entries.first().row to outcome.entries.first().column)
    }

    @Test
    fun `a grid left scrolled with nothing selected begins on the first whole row in view`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, scrolledRows = 1.4))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((14 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `a selected piece on a scrolled grid is where the scan begins`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, scrolledRows = 2.0, selected = 18))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((18 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `tiles that print no word are found by the kind's own sight of them, to the grid's last row`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 31, selected = 0, wordless = true), FacedPieceScan())

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 31).toList(), outcome.pieces())
    }

    @Test
    fun `a framed tile is begun on though no tile prints a word`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, scrolledRows = 2.0, selected = 18, wordless = true), FacedPieceScan())

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((18 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `with no tile framed, a kind that finds no row without one has nowhere to begin`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 30, wordless = true), FacedPieceScan())

        assertIs<Outcome.Stopped<Int>>(outcome)
        assertEquals(Outcome.Reason.STORAGE_NOT_OPEN, outcome.reason)
    }

    @Test
    fun `a row whose numbers come back as one line still has every tile`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 30, runsNumbersTogether = true))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
    }

    @Test
    fun `a gesture the player's finger cancelled ends the scan as cancelled, not failed, what was read kept`() = runTest {
        val storage = FakeStorage(pieces = 30)
        val touched = object : Touch by storage {
            private var taps = 0

            override suspend fun tap(x: Int, y: Int) {
                if (++taps == 4) throw CancellationException("a finger touched the screen")
                storage.tap(x, y)
            }
        }
        val entries = mutableListOf<ScanEntry<Int>>()

        assertFailsWith<CancellationException> { PieceScan(storage.layout).run(storage, touched, storage, keeper, entries, 0) }
        assertEquals((0 until 3).toList(), entries.map { it.card })
    }

    @Test
    fun `each entry reaches the keeper the moment it is read, so a scan cut short has handed over every one`() = runTest {
        val storage = FakeStorage(pieces = 30)
        val touched = object : Touch by storage {
            private var taps = 0

            override suspend fun tap(x: Int, y: Int) {
                if (++taps == 12) throw CancellationException("the process is going")
                storage.tap(x, y)
            }
        }

        assertFailsWith<CancellationException> { PieceScan(storage.layout).run(storage, touched, storage, keeper, mutableListOf(), 0) }
        assertEquals((0 until 11).toList(), journal.map { it.card })
        assertEquals((0 until 11).toList(), journal.map { it.index })
    }

    @Test
    fun `a tile tap the game dropped is tapped again, and every piece read in its place`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 10, droppedTileTaps = setOf(2)))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 10).toList(), outcome.pieces())
    }

    @Test
    fun `a tile whose frame does not show after a second tap stops the scan, the grid lost`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 10, droppedTileTaps = setOf(2, 3)))

        assertIs<Outcome.Stopped<Int>>(outcome)
        assertEquals(Outcome.Reason.GRID_LOST, outcome.reason)
        assertEquals(listOf(0), outcome.pieces())
    }

    @Test
    fun `a drag after which the grid is found neither by its frame nor its words stops the scan, what was read kept`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, leavesOnDrag = 1))

        assertIs<Outcome.Stopped<Int>>(outcome)
        assertEquals(Outcome.Reason.GRID_LOST, outcome.reason)
        assertTrue(outcome.entries.isNotEmpty())
    }

    @Test
    fun `a failure mid-scan ends it failed, what was read kept`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 30, failsOnCapture = 6))

        assertIs<Outcome.Failed<Int>>(outcome)
        assertEquals("the display delivered no frame", outcome.cause.message)
        assertTrue(outcome.entries.isNotEmpty())
    }

    @Test
    fun `without the header's count the storage is not open`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 5, storageOpen = false))

        assertIs<Outcome.Stopped<Int>>(outcome)
        assertEquals(Outcome.Reason.STORAGE_NOT_OPEN, outcome.reason)
    }

    @Test
    fun `a panel the reader cannot close is kept as an image and the scan goes on`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 5, unreadable = setOf(2)))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals(listOf(2), panels)
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
                ended = scanOver(storage, entries = entries, settleMillis = 1_000)
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
        val outcome = scanOver(FakeStorage(pieces = 30, unlabelled = setOf(4, 13, 28)))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
    }

    @Test
    fun `a last tile whose number the recogniser missed is not read, and nothing says so`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 31, unlabelled = setOf(30)))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
    }

    @Test
    fun `a grid that keeps gliding after the drag is registered once it stops`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, rowsPerDrag = 0.4, glideRows = 0.6))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `tiles that all print the same number are registered by the framed tile instead`() = runTest {
        val outcome = scanOver(FakeStorage(pieces = 42, sameNumbers = true))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `a panel with tabs is read under each, every piece in its place though each switch re-centres the grid`() = runTest {
        val storage = FakeStorage(pieces = 30, tabCount = 3)

        val outcome = scanOver(storage, TabbedPieceScan(storage.tabs))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
        outcome.entries.forEach { entry ->
            assertEquals(entry.underEveryTab(3), entry.rows)
            assertEquals(entry.card / 7 to entry.card % 7, entry.row to entry.column)
        }
    }

    @Test
    fun `a tab whose grid lists other tiles does not lose the piece`() = runTest {
        val storage = FakeStorage(pieces = 30, tabCount = 3, ownListTab = 2)

        val outcome = scanOver(storage, TabbedPieceScan(storage.tabs))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 30).toList(), outcome.pieces())
    }

    @Test
    fun `a tab tap the game dropped is tapped again`() = runTest {
        val storage = FakeStorage(pieces = 10, tabCount = 3, droppedTabTaps = setOf(1, 5))

        val outcome = scanOver(storage, TabbedPieceScan(storage.tabs))

        assertIs<Outcome.Finished<Int>>(outcome)
        outcome.entries.forEach { entry -> assertEquals(entry.underEveryTab(3), entry.rows) }
    }

    @Test
    fun `a switch that moves the grid down is regripped as well as one that moves it up`() = runTest {
        val storage = FakeStorage(pieces = 42, scrolledRows = 2.0, selected = 18, tabCount = 2)

        val outcome = scanOver(storage, TabbedPieceScan(storage.tabs))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((18 until 42).toList(), outcome.pieces())
    }

    @Test
    fun `the scan finishes at the piece the kind says is beyond, which is not kept`() = runTest {
        val storage = FakeStorage(pieces = 30, tabCount = 3)

        val outcome = scanOver(storage, TabbedPieceScan(storage.tabs, endsAt = 12))

        assertIs<Outcome.Finished<Int>>(outcome)
        assertEquals((0 until 12).toList(), outcome.pieces())
    }

    @Test
    fun `a kind that loses the grid stops the scan, what it read before kept`() = runTest {
        val storage = FakeStorage(pieces = 30, tabCount = 3, leavesOnTabTap = 5)

        val outcome = scanOver(storage, TabbedPieceScan(storage.tabs))

        assertIs<Outcome.Stopped<Int>>(outcome)
        assertEquals(Outcome.Reason.GRID_LOST, outcome.reason)
        assertEquals(listOf(0), outcome.pieces())
    }

    @Test
    fun `the tile a kind is lent is the one tapped, around where the tap landed`() = runTest {
        val storage = FakeStorage(pieces = 10)
        val tiles = mutableListOf<Box>()
        val scan = object : PieceScan() {
            override suspend fun readTile(tapped: Tapped): Read<Int> = super.readTile(tapped).also { tiles += tapped.tile }
        }

        scanOver(storage, scan)

        assertEquals(storage.taps.size, tiles.size)
        storage.taps.zip(tiles).forEach { (tap, tile) ->
            assertEquals(tap.first, (tile.left + tile.right) / 2)
            /* The frame's line is a few pixels thick, so its centre is known to within them. */
            assertTrue(abs(tap.second - (tile.top + tile.bottom) / 2) <= 5, "tapped at $tap, lent $tile")
        }
    }

    @Test
    fun `a tab still drawing its panel is read once it has drawn`() = runTest {
        val storage = FakeStorage(pieces = 10, tabCount = 3, drawsTabsOver = 2)

        val outcome = scanOver(storage, TabbedPieceScan(storage.tabs))

        assertIs<Outcome.Finished<Int>>(outcome)
        outcome.entries.forEach { entry -> assertEquals(entry.underEveryTab(3), entry.rows) }
    }
}
