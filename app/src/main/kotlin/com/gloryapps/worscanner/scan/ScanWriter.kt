package com.gloryapps.worscanner.scan

import android.content.Context
import android.graphics.Bitmap
import com.gloryapps.worscanner.capture.BitmapFrame
import com.gloryapps.worscanner.capture.STAMP
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.kinds.Scannable
import com.gloryapps.worscanner.scanner.scan.Keeper
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import com.gloryapps.worscanner.scanner.senses.Frame
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

/** A scan as the lab reads it: what was scanned, the display it ran on, how it ended, and one entry per tile. */
@Serializable
data class ScanFile<T>(
    @EncodeDefault val version: Int = 2,
    /** A `Kind` id; `entries[].card` is shaped by it. */
    val kind: String,
    val startedAt: String,
    val width: Int,
    val height: Int,
    val outcome: String,
    /** Why it stopped, and the rows of the frame it stopped on, where it did. */
    val detail: String? = null,
    val seen: List<String> = emptyList(),
    val entries: List<ScanEntry<T>>,
)

/** One folder per scan: `scan.json` and the panel of every tile the reader did not close. */
class ScanWriter<T>(private val context: Context, private val kind: Kind, private val scannable: Scannable<T>) {
    private val json = Json { prettyPrint = true }
    val stamp: String = LocalDateTime.now().format(STAMP)
    private val folder = File(File(context.getExternalFilesDir(null), "scans"), stamp).apply { mkdirs() }

    val keeper = Keeper { frame: Frame, index: Int ->
        val bitmap = (frame as BitmapFrame).bitmap
        val box = scannable.layout.panel.box(frame.width, frame.height)
        val panel = Bitmap.createBitmap(bitmap, box.left, box.top, box.right - box.left, box.bottom - box.top)
        val file = File(folder, "$index.png")
        file.outputStream().use { panel.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.name
    }

    fun write(first: BitmapFrame, outcome: Outcome<T>): File {
        val (ended, entries) = when (outcome) {
            is Outcome.Finished -> "finished" to outcome.entries
            is Outcome.Stopped -> "stopped:${outcome.reason.name.lowercase()}" to outcome.entries
            is Outcome.Failed -> "failed:${outcome.cause}" to outcome.entries
        }
        val stopped = outcome as? Outcome.Stopped
        val scan = ScanFile(
            kind = kind.id,
            startedAt = stamp,
            width = first.width,
            height = first.height,
            outcome = ended,
            detail = stopped?.detail ?: (outcome as? Outcome.Failed)?.cause?.stackTraceToString()?.lineSequence()?.take(4)?.joinToString(" | "),
            seen = stopped?.seen ?: (outcome as? Outcome.Finished)?.seen.orEmpty(),
            entries = entries,
        )
        /* A scan that read nothing leaves the frame it looked at, which is what tells why. */
        if (entries.isEmpty()) File(folder, "first.png").outputStream().use { first.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        return File(folder, "scan.json").apply { writeText(json.encodeToString(ScanFile.serializer(scannable.serializer), scan)) }
    }
}
