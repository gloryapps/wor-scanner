package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Keeper
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import com.gloryapps.worscanner.scanner.senses.Frame
import java.io.File
import java.time.LocalDateTime

/** One folder per scan: `scan.json` and the panel of every tile the reader did not close, and its journal until the file is written. */
class ScanWriter<T>(scans: File, private val kind: Kind, private val scan: Scan<T>, private val pictures: Pictures) : Keeper<T> {
    val stamp: String = LocalDateTime.now().format(STAMP)
    /** Made when the first file goes in, so a scan that wrote nothing leaves no folder. */
    private val folder by lazy { File(scans, stamp).apply { mkdirs() } }
    private val journal by lazy { Journal(folder, scan.serializer) }

    /* A tile read under several tabs keeps its panels side by side, in the order they were read. */
    override suspend fun panel(frames: List<Frame>, index: Int): String {
        val file = File(folder, "$index.png")
        pictures.write(frames.map { Cut(it, scan.layout.panel.box(it.width, it.height)) }, file)

        return file.name
    }

    /** Opens the journal once the scan has its first frame. */
    fun begin(first: Frame) =
        journal.open(fileOf(first, Outcome.Stopped(Outcome.Reason.INTERRUPTED, emptyList(), "the app was closed before the scan ended")))

    override suspend fun entry(entry: ScanEntry<T>) = journal.append(entry)

    /** The file the lab reads, which closes the journal; a scan that never saw a frame writes a display of 0x0. */
    fun write(first: Frame?, outcome: Outcome<T>) {
        /* A scan that read nothing leaves the frame it looked at, which is what tells why. */
        if (outcome.entries.isEmpty() && first != null) pictures.write(first, File(folder, "first.png"))
        writeScan(folder, fileOf(first, outcome), scan.serializer)
        journal.close()
    }

    private fun fileOf(first: Frame?, outcome: Outcome<T>): ScanFile<T> {
        val stopped = outcome as? Outcome.Stopped

        return ScanFile(
            kind = kind.id,
            startedAt = stamp,
            width = first?.width ?: 0,
            height = first?.height ?: 0,
            outcome = outcome.wire(),
            detail = stopped?.detail ?: (outcome as? Outcome.Failed)?.cause?.stackTraceToString()?.lineSequence()?.take(4)?.joinToString(" | "),
            seen = stopped?.seen ?: (outcome as? Outcome.Finished)?.seen.orEmpty(),
            entries = outcome.entries,
        )
    }
}
