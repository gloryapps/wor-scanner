package com.gloryapps.worscanner.capture

import android.content.Context
import android.graphics.Bitmap
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.scanner.text.Line
import com.gloryapps.worscanner.scan.ScanFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** One frame read as a kind: its size, every line the recogniser gave up, and the record the reader made of them. */
@Serializable
data class Reading<T>(val kind: String, val width: Int, val height: Int, val lines: List<Line>, val card: T)

/** Something kept on disk under a stamp: a reading's JSON and PNG, or a scan's folder with what it came to. */
data class Kept(
    val stamp: String,
    /** `reading` or `scan`. */
    val form: String,
    val files: List<File>,
    /** What was read; null where the JSON names a kind this app does not know, or will not read at all. */
    val kind: Kind? = null,
    val entries: Int? = null,
    val outcome: String? = null,
    /** Why a scan stopped or failed, in the scan's own words; null when it finished. */
    val detail: String? = null,
) {
    val at: LocalDateTime get() = LocalDateTime.parse(stamp, STAMP)
}

val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

/** Where readings and scans go to be looked at later: the app's own external folder. */
class Readings(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val readings: File get() = File(context.getExternalFilesDir(null), "readings").apply { mkdirs() }
    private val scans: File get() = File(context.getExternalFilesDir(null), "scans").apply { mkdirs() }

    fun <T> keep(frame: BitmapFrame, reading: Reading<T>, card: KSerializer<T>): Kept {
        val stamp = LocalDateTime.now().format(STAMP)
        val png = File(readings, "$stamp.png")
        val text = File(readings, "$stamp.json")
        png.outputStream().use { frame.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        text.writeText(json.encodeToString(Reading.serializer(card), reading))

        return Kept(stamp, "reading", listOf(text, png), kindOf(reading.kind))
    }

    /** The JSON of a kept thing, as written. */
    fun text(kept: Kept): String = kept.files.first { it.extension == "json" }.readText()

    /** A kept scan opened, every card left as the JSON it is; null for a reading, and for a file that will not read. */
    fun opened(kept: Kept): ScanFile<JsonElement>? = kept.files.firstOrNull { it.name == "scan.json" }?.let(::scanIn)

    fun delete(kept: Kept) {
        kept.files.forEach { it.delete() }
        if (kept.form == "scan") File(scans, kept.stamp).delete()
    }

    /** Newest first, which is the one the reader came to look at. */
    fun list(): List<Kept> {
        val read = readings.listFiles { file -> file.extension == "json" }.orEmpty().map {
            Kept(it.nameWithoutExtension, "reading", listOf(it, File(readings, "${it.nameWithoutExtension}.png")), kindIn(it))
        }
        /* A folder without its JSON is a scan still running; the list wants the count and the outcome, not the cards, so the card stays whatever JSON it is. */
        val scanned = scans.listFiles { file -> file.isDirectory && File(file, "scan.json").exists() }.orEmpty().map { folder ->
            val scan = scanIn(File(folder, "scan.json"))
            Kept(folder.name, "scan", folder.listFiles().orEmpty().sortedBy { it.name }, kindOf(scan?.kind), scan?.entries?.size, scan?.outcome, scan?.detail)
        }

        return (read + scanned).sortedByDescending { it.stamp }
    }

    private fun scanIn(file: File): ScanFile<JsonElement>? =
        resultOf { json.decodeFromString(ScanFile.serializer(JsonElement.serializer()), file.readText()) }.getOrNull()

    /** The kind a reading names, which is all the list wants of a file it does not otherwise open. */
    private fun kindIn(file: File): Kind? =
        kindOf(resultOf { json.parseToJsonElement(file.readText()).jsonObject["kind"]?.jsonPrimitive?.content }.getOrNull())

    private fun kindOf(id: String?): Kind? = Kind.entries.firstOrNull { it.id == id }
}
