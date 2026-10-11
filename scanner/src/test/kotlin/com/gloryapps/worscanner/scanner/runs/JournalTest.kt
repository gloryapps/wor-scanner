package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JournalTest {
    private val folder: File = createTempDirectory().toFile()
    private val journal = Journal(folder, Int.serializer())
    private val unended = ScanFile<Int>(
        kind = "gear",
        startedAt = "20260927-120000",
        width = 1280,
        height = 720,
        outcome = Outcome.Stopped<Int>(Outcome.Reason.INTERRUPTED, emptyList(), "closed").wire(),
        detail = "closed",
        entries = emptyList(),
    )

    private fun entry(index: Int) = ScanEntry(index, index / 7, index % 7, card = 100 + index, rows = listOf("Piece $index"))

    private fun closed(): ScanFile<JsonElement> {
        Journal.closeIn(folder)

        return readScan(folder, JsonElement.serializer())
    }

    @Test
    fun `a scan whose process died before its file was written gets the file the journal opened with, every entry in it`() {
        journal.open(unended)
        (0 until 12).forEach { journal.append(entry(it)) }

        val scan = closed()

        assertEquals("stopped:interrupted", scan.outcome)
        assertEquals(Ended.STOPPED, endedOf(scan.outcome))
        assertEquals((100 until 112).toList(), scan.entries.map { it.card.jsonPrimitive.int })
        assertEquals(listOf("Piece 11"), scan.entries.last().rows)
        assertFalse(Journal.heldBy(folder))
    }

    @Test
    fun `a line the death cut short is left out, every whole one before it kept`() {
        journal.open(unended)
        (0 until 3).forEach { journal.append(entry(it)) }
        File(folder, "journal.jsonl").appendText("""{"index":3,"row":0,"col""")

        assertEquals(listOf(0, 1, 2), closed().entries.map { it.index })
    }

    @Test
    fun `a journal whose first line the death cut short goes, leaving no file`() {
        File(folder, "journal.jsonl").writeText("""{"version":2,"kind":"ge""")

        Journal.closeIn(folder)

        assertFalse(Journal.heldBy(folder))
        assertFalse(File(folder, "scan.json").exists())
    }

    @Test
    fun `a scan that wrote its file before dying keeps it, and only the journal goes`() {
        journal.open(unended)
        journal.append(entry(0))
        writeScan(folder, unended.copy(outcome = "finished", entries = listOf(entry(0), entry(1))), Int.serializer())

        val scan = closed()

        assertEquals("finished", scan.outcome)
        assertEquals(2, scan.entries.size)
        assertFalse(Journal.heldBy(folder))
    }

    @Test
    fun `a file the death cut short while it was written is replaced by the journal's`() {
        journal.open(unended)
        (0 until 2).forEach { journal.append(entry(it)) }
        File(folder, "scan.json").writeText("""{"version":2,"kind":"gear","entr""")

        val scan = closed()

        assertEquals("stopped:interrupted", scan.outcome)
        assertEquals(2, scan.entries.size)
    }
}
