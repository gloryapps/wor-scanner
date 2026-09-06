package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.Frame
import com.gloryapps.worscanner.scanner.catalogue.Slot
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WalkTest {
    private val kept = mutableListOf<Int>()
    private val keeper = Keeper { _: Frame, entry -> kept += entry.index; "${entry.index}.png" }

    private suspend fun Walk.run() = run(mutableListOf())

    private fun walkOver(storage: FakeStorage) = Walk(storage, storage, storage, keeper, settleMillis = 0)

    @Test
    fun `every piece on one screen is tapped once, in reading order`() = runTest {
        val storage = FakeStorage(pieces = 10)

        val outcome = walkOver(storage).run()

        assertIs<Outcome.Finished>(outcome)
        assertEquals((0 until 10).toList(), outcome.entries.map { it.index })
        assertEquals(10, storage.taps.size)
        assertEquals(0, storage.drags)
        assertEquals(174 to 285, storage.taps.first())
        assertEquals(254 to 471, storage.taps[8])
    }

    @Test
    fun `each entry holds the panel of the piece it tapped`() = runTest {
        val outcome = walkOver(FakeStorage(pieces = 30)).run()

        assertIs<Outcome.Finished>(outcome)
        outcome.entries.forEach { entry ->
            assertEquals("cataclysm", entry.card.set)
            assertEquals(Slot.RING, entry.card.slot)
            assertEquals(entry.index.toDouble(), entry.card.attributes.single().value)
        }
    }

    @Test
    fun `the grid is dragged when the visible rows are done and the walk goes on where it landed`() = runTest {
        val storage = FakeStorage(pieces = 30)

        val outcome = walkOver(storage).run()

        assertIs<Outcome.Finished>(outcome)
        assertEquals(30, outcome.entries.size)
        assertEquals((0 until 30).toList(), outcome.entries.map { it.index })
        assertTrue(storage.drags >= 1)
    }

    @Test
    fun `a drag that stops between two rows is found out by the tiles, not assumed`() = runTest {
        val storage = FakeStorage(pieces = 35, rowsPerDrag = 1.4)

        val outcome = walkOver(storage).run()

        assertIs<Outcome.Finished>(outcome)
        assertEquals((0 until 35).toList(), outcome.entries.map { it.index })
        assertEquals((0 until 35).map { it.toDouble() }, outcome.entries.map { it.card.attributes.single().value })
    }

    @Test
    fun `a drag that overshoots is found out just the same`() = runTest {
        val storage = FakeStorage(pieces = 42, rowsPerDrag = 1.6)

        val outcome = walkOver(storage).run()

        assertIs<Outcome.Finished>(outcome)
        assertEquals((0 until 42).toList(), outcome.entries.map { it.index })
        assertEquals((0 until 42).map { it.toDouble() }, outcome.entries.map { it.card.attributes.single().value })
    }

    @Test
    fun `the last rows settle on the viewport's floor and are still read`() = runTest {
        val storage = FakeStorage(pieces = 29)

        val outcome = walkOver(storage).run()

        assertIs<Outcome.Finished>(outcome)
        assertEquals((0 until 29).toList(), outcome.entries.map { it.index })
        assertEquals((0 until 29).map { it.toDouble() }, outcome.entries.map { it.card.attributes.single().value })
    }

    @Test
    fun `without the header's count the storage is not open`() = runTest {
        val outcome = walkOver(FakeStorage(pieces = 5, storageOpen = false)).run()

        assertIs<Outcome.Stopped>(outcome)
        assertEquals(Outcome.Reason.STORAGE_NOT_OPEN, outcome.reason)
    }

    @Test
    fun `a panel the reader cannot close is kept as an image and the walk goes on`() = runTest {
        val outcome = walkOver(FakeStorage(pieces = 5, unreadable = setOf(2))).run()

        assertIs<Outcome.Finished>(outcome)
        assertEquals(listOf(2), kept)
        assertEquals("2.png", outcome.entries[2].png)
        assertEquals(5, outcome.entries.size)
    }

    @Test
    fun `cancelling ends the walk by its exception and leaves the caller what was read so far`() = runTest {
        val storage = FakeStorage(pieces = 30)
        val entries = mutableListOf<ScanEntry>()
        var ended: Outcome? = null

        coroutineScope {
            val job = launch {
                ended = Walk(storage, storage, storage, keeper, settleMillis = 1_000).run(entries)
            }
            testScheduler.advanceTimeBy(3_500)
            job.cancel()
            job.join()
        }

        assertNull(ended)
        assertEquals(3, entries.size)
    }
}
