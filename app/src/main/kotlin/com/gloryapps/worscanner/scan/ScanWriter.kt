package com.gloryapps.worscanner.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.gloryapps.worscanner.capture.BitmapFrame
import com.gloryapps.worscanner.capture.STAMP
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Keeper
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.Scan
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
class ScanWriter<T>(private val context: Context, private val kind: Kind, private val scan: Scan<T>) {
    private val json = Json { prettyPrint = true }
    val stamp: String = LocalDateTime.now().format(STAMP)
    private val folder = File(File(context.getExternalFilesDir(null), "scans"), stamp).apply { mkdirs() }

    /* A tile read under several tabs keeps its panels side by side, in the order they were read. */
    val keeper = Keeper { frames: List<Frame>, index: Int ->
        val panels = frames.map { frame ->
            val box = scan.layout.panel.box(frame.width, frame.height)
            Bitmap.createBitmap((frame as BitmapFrame).bitmap, box.left, box.top, box.right - box.left, box.bottom - box.top)
        }
        val sheet = Bitmap.createBitmap(panels.sumOf { it.width }, panels.maxOf { it.height }, Bitmap.Config.ARGB_8888)
        Canvas(sheet).let { canvas -> panels.fold(0f) { left, panel -> canvas.drawBitmap(panel, left, 0f, null); left + panel.width } }
        val file = File(folder, "$index.png")
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.name
    }

    fun write(first: BitmapFrame, outcome: Outcome<T>): File {
        val (ended, entries) = when (outcome) {
            is Outcome.Finished -> "finished" to outcome.entries
            is Outcome.Stopped -> "stopped:${outcome.reason.name.lowercase()}" to outcome.entries
            is Outcome.Failed -> "failed:${outcome.cause}" to outcome.entries
        }
        val stopped = outcome as? Outcome.Stopped
        val file = ScanFile(
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

        return File(folder, "scan.json").apply { writeText(json.encodeToString(ScanFile.serializer(scan.serializer), file)) }
    }
}
