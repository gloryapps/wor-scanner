package com.gloryapps.worscanner.scan

import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File

/**
 * A scan until its file is written: a first line holding the file as it reads should the process die
 * before the scan ends, then one line per entry, appended as it is read.
 */
class Journal<T>(folder: File, private val card: KSerializer<T>) {
    private val file = File(folder, NAME)

    fun open(unended: ScanFile<T>) = file.writeText(LINE.encodeToString(ScanFile.serializer(card), unended) + "\n")

    fun append(entry: ScanEntry<T>) = file.appendText(LINE.encodeToString(ScanEntry.serializer(card), entry) + "\n")

    /** Once the scan's file holds everything the journal did. */
    fun close() {
        file.delete()
    }

    companion object {
        private const val NAME = "journal.jsonl"
        private val LINE = Json { ignoreUnknownKeys = true }

        /** Whether a scan's folder holds a journal no scan has closed. */
        fun heldBy(folder: File): Boolean = File(folder, NAME).exists()

        /**
         * Closes the journal a scan left in its folder, its process dead before the scan ended. Where the
         * scan's file was not written, or not to its end, it becomes the one the journal opened with,
         * holding every entry after it but a line the death cut short.
         */
        fun closeIn(folder: File) {
            val journal = File(folder, NAME)
            if (resultOf { readScan(folder, JsonElement.serializer()) }.isFailure) {
                val lines = journal.readLines()
                val opened = LINE.decodeFromString(ScanFile.serializer(JsonElement.serializer()), lines.first())
                val entries = lines.drop(1).mapNotNull { resultOf { LINE.decodeFromString(ScanEntry.serializer(JsonElement.serializer()), it) }.getOrNull() }
                writeScan(folder, opened.copy(entries = entries), JsonElement.serializer())
            }
            journal.delete()
        }
    }
}
