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
import kotlinx.serialization.KSerializer
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

/** How a scan ended, as the lab and the app's list read it. */
enum class Ended { FINISHED, STOPPED, FAILED }

/** The outcome as the file says it: `finished`, `stopped:<reason>`, `failed:<cause>`. */
fun Outcome<*>.wire(): String = when (this) {
    is Outcome.Finished -> "finished"
    is Outcome.Stopped -> "stopped:${reason.name.lowercase()}"
    is Outcome.Failed -> "failed:$cause"
}

/** How a file's outcome ended; null for a word no scan writes. */
fun endedOf(wire: String): Ended? = Ended.entries.firstOrNull { wire.substringBefore(':') == it.name.lowercase() }

/** One folder per scan: `scan.json` and the panel of every tile the reader did not close, and its journal until the file is written. */
class ScanWriter<T>(private val context: Context, private val kind: Kind, private val scan: Scan<T>) : Keeper<T> {
    private val stamp: String = LocalDateTime.now().format(STAMP)
    /** Made when the first file goes in, so a scan that wrote nothing leaves no folder. */
    private val folder by lazy { File(File(context.getExternalFilesDir(null), "scans"), stamp).apply { mkdirs() } }
    private val journal by lazy { Journal(folder, scan.serializer) }

    /* A tile read under several tabs keeps its panels side by side, in the order they were read. */
    override suspend fun panel(frames: List<Frame>, index: Int): String {
        val panels = frames.map { frame ->
            val box = scan.layout.panel.box(frame.width, frame.height)
            Bitmap.createBitmap((frame as BitmapFrame).bitmap, box.left, box.top, box.right - box.left, box.bottom - box.top)
        }
        val sheet = Bitmap.createBitmap(panels.sumOf { it.width }, panels.maxOf { it.height }, Bitmap.Config.ARGB_8888)
        Canvas(sheet).let { canvas -> panels.fold(0f) { left, panel -> canvas.drawBitmap(panel, left, 0f, null); left + panel.width } }
        val file = File(folder, "$index.png")
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }

        return file.name
    }

    /** Opens the journal once the scan has its first frame. */
    fun begin(first: BitmapFrame) =
        journal.open(fileOf(first, Outcome.Stopped(Outcome.Reason.INTERRUPTED, emptyList(), "the app was closed before the scan ended")))

    override suspend fun entry(entry: ScanEntry<T>) = journal.append(entry)

    /** The file the lab reads, which closes the journal; a scan that never saw a frame writes a display of 0x0. */
    fun write(first: BitmapFrame?, outcome: Outcome<T>) {
        /* A scan that read nothing leaves the frame it looked at, which is what tells why. */
        if (outcome.entries.isEmpty() && first != null) File(folder, "first.png").outputStream().use { first.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        writeScan(folder, fileOf(first, outcome), scan.serializer)
        journal.close()
    }

    private fun fileOf(first: BitmapFrame?, outcome: Outcome<T>): ScanFile<T> {
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

fun <T> writeScan(folder: File, scan: ScanFile<T>, card: KSerializer<T>) = File(folder, SCAN).writeText(JSON.encodeToString(ScanFile.serializer(card), scan))

fun <T> readScan(folder: File, card: KSerializer<T>): ScanFile<T> = JSON.decodeFromString(ScanFile.serializer(card), File(folder, SCAN).readText())

private const val SCAN = "scan.json"
private val JSON = Json { prettyPrint = true; ignoreUnknownKeys = true }
