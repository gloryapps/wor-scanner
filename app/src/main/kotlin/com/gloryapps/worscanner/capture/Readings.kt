package com.gloryapps.worscanner.capture

import android.content.Context
import android.graphics.Bitmap
import com.gloryapps.worscanner.scanner.reading.Line
import com.gloryapps.worscanner.scanner.reading.ScannedGear
import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.walk.Scan
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** One frame read: its size, every line the recogniser gave up, and the card the reader made of them. */
@Serializable
data class Reading(val width: Int, val height: Int, val lines: List<Line>, val card: ScannedGear)

/** Something kept on disk under a stamp: a reading's JSON and PNG, or a scan's folder with what it came to. */
data class Kept(val stamp: String, val kind: String, val files: List<File>, val pieces: Int? = null, val outcome: String? = null) {
    val at: LocalDateTime get() = LocalDateTime.parse(stamp, STAMP)
}

val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

/** Where readings and scans go to be looked at later: the app's own external folder. */
class Readings(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val readings: File get() = File(context.getExternalFilesDir(null), "readings").apply { mkdirs() }
    private val scans: File get() = File(context.getExternalFilesDir(null), "scans").apply { mkdirs() }

    fun keep(frame: BitmapFrame, reading: Reading): Kept {
        val stamp = LocalDateTime.now().format(STAMP)
        val png = File(readings, "$stamp.png")
        val text = File(readings, "$stamp.json")
        png.outputStream().use { frame.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        text.writeText(json.encodeToString(reading))

        return Kept(stamp, "reading", listOf(text, png))
    }

    /** The JSON of a kept thing, as written. */
    fun text(kept: Kept): String = kept.files.first { it.extension == "json" }.readText()

    fun delete(kept: Kept) {
        kept.files.forEach { it.delete() }
        if (kept.kind == "scan") File(scans, kept.stamp).delete()
    }

    /** Newest first, which is the one the reader came to look at. */
    fun list(): List<Kept> {
        val read = readings.listFiles { file -> file.extension == "json" }.orEmpty().map {
            Kept(it.nameWithoutExtension, "reading", listOf(it, File(readings, "${it.nameWithoutExtension}.png")))
        }
        val scanned = scans.listFiles { file -> file.isDirectory }.orEmpty().map { folder ->
            val scan = File(folder, "scan.json").takeIf { it.exists() }?.let { resultOf { json.decodeFromString<Scan>(it.readText()) }.getOrNull() }
            Kept(folder.name, "scan", folder.listFiles().orEmpty().sortedBy { it.name }, scan?.entries?.size, scan?.outcome)
        }

        return (read + scanned).sortedByDescending { it.stamp }
    }
}
