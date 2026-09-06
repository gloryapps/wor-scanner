package com.gloryapps.worscanner.capture

import android.content.Context
import android.graphics.Bitmap
import com.gloryapps.worscanner.scanner.reading.Line
import com.gloryapps.worscanner.scanner.reading.ScannedGear
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** One frame read: its size, every line the recogniser gave up, and the card the reader made of them. */
@Serializable
data class Reading(val width: Int, val height: Int, val lines: List<Line>, val card: ScannedGear)

/** A reading on disk: the JSON and the PNG that share a stamp. */
data class Kept(val stamp: String, val json: File, val png: File) {
    val files: List<File> get() = listOf(json, png)
}

/** Where a reading goes to be looked at later: the app's own external folder, a PNG and a JSON per stamp. */
class Readings(private val context: Context) {
    private val json = Json { prettyPrint = true }
    private val folder: File get() = File(context.getExternalFilesDir(null), "readings").apply { mkdirs() }

    fun keep(frame: BitmapFrame, reading: Reading): Kept {
        val stamp = LocalDateTime.now().format(STAMP)
        val kept = at(stamp)
        kept.png.outputStream().use { frame.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        kept.json.writeText(json.encodeToString(reading))

        return kept
    }

    /** Newest first, which is the one the reader came to look at. */
    fun list(): List<Kept> = folder.listFiles { file -> file.extension == "json" }
        .orEmpty()
        .map { at(it.nameWithoutExtension) }
        .sortedByDescending { it.stamp }

    private fun at(stamp: String) = Kept(stamp, File(folder, "$stamp.json"), File(folder, "$stamp.png"))

    private companion object {
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
