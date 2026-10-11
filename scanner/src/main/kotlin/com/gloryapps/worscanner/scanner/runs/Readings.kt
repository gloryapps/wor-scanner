package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Line
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/** One frame read as a kind, as its file holds it: its size, every line the recogniser gave up, and the record the reader made of them. */
@Serializable
data class ReadingFile<T>(val kind: String, val width: Int, val height: Int, val lines: List<Line>, val card: T)

/** Something kept on disk under a stamp: a Read's JSON and the screen it was read from, or a scan's folder with what it came to. */
sealed interface Kept {
    val stamp: String
    val files: List<File>
    /** What was read; null where the JSON names a kind this app does not know, or will not read at all. */
    val kind: Kind?
    val at: LocalDateTime get() = LocalDateTime.parse(stamp, STAMP)

    /** One frame the overlay's Read kept. */
    data class Read(override val stamp: String, override val files: List<File>, override val kind: Kind? = null) : Kept

    data class Scan(
        override val stamp: String,
        override val files: List<File>,
        override val kind: Kind? = null,
        val entries: Int? = null,
        /** Null where the file does not say, or will not read. */
        val ended: Ended? = null,
        /** Why it stopped or failed, in the scan's own words; null when it finished. */
        val detail: String? = null,
    ) : Kept
}

/** The JSON of a scan, which is what the lab imports; a Read's is not one. */
fun Kept.scans(): List<File> = if (this is Kept.Scan) files.filter { it.extension == "json" } else emptyList()

val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

/**
 * Where readings and scans go to be looked at later: the folder `root` the platform gives the app, read
 * and written off the main thread. Made as the process starts, when it closes the journals of the scans
 * the last one died in the middle of, before anything is listed.
 */
class Readings(private val root: File, private val pictures: Pictures, reports: Reports) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val readings: File get() = File(root, "readings").apply { mkdirs() }
    private val scans: File get() = File(root, "scans").apply { mkdirs() }
    /** What the list reads of each scan, by stamp: a scan's file is written once and never changed. */
    private val summaries = ConcurrentHashMap<String, Kept>()
    /* No scan of this process holds a journal yet: the platform makes this before any scan can start. */
    private val recovered = CoroutineScope(Dispatchers.IO).async {
        scans.listFiles { file -> Journal.heldBy(file) }.orEmpty().forEach { folder ->
            resultOf { Journal.closeIn(folder) }.onFailure { reports.failed("closing the journal of scan ${folder.name}", it) }
        }
    }

    /** Where a new scan of the kind writes, in a folder of its own beside the others. */
    fun <T> writer(kind: Kind, scan: Scan<T>): ScanWriter<T> = ScanWriter(scans, kind, scan, pictures)

    suspend fun <T> keep(frame: Frame, reading: ReadingFile<T>, card: KSerializer<T>): Kept = withContext(Dispatchers.IO) {
        val stamp = LocalDateTime.now().format(STAMP)
        val png = File(readings, "$stamp.png")
        val text = File(readings, "$stamp.json")
        pictures.write(frame, png)
        text.writeText(json.encodeToString(ReadingFile.serializer(card), reading))

        Kept.Read(stamp, listOf(text, png), kindOf(reading.kind))
    }

    /** The JSON of a kept thing, as written. */
    suspend fun text(kept: Kept): String = withContext(Dispatchers.IO) { kept.files.first { it.extension == "json" }.readText() }

    /** A kept scan opened, every card left as the JSON it is; null for a reading, and for a file that will not read. */
    suspend fun opened(kept: Kept): ScanFile<JsonElement>? = withContext(Dispatchers.IO) { (kept as? Kept.Scan)?.let { scanIn(File(scans, it.stamp)) } }

    suspend fun delete(kept: Kept) = withContext(Dispatchers.IO) {
        kept.files.forEach { it.delete() }
        if (kept is Kept.Scan) {
            File(scans, kept.stamp).delete()
            summaries.remove(kept.stamp)
        }
    }

    /** Clears the folders scans left without a file their journal could give them: panels no list shows. Only while no scan runs. */
    suspend fun sweep() = withContext(Dispatchers.IO) {
        scans.listFiles { file -> file.isDirectory && !File(file, "scan.json").exists() }.orEmpty().forEach { it.deleteRecursively() }
    }

    /** Newest first, which is the one the reader came to look at. */
    suspend fun list(): List<Kept> = withContext(Dispatchers.IO) {
        recovered.await()
        val read = readings.listFiles { file -> file.extension == "json" }.orEmpty().map(::readingIn)
        /* A folder without its JSON is a scan still running. */
        val scanned = scans.listFiles { file -> file.isDirectory && File(file, "scan.json").exists() }.orEmpty().map(::scanKeptIn)

        (read + scanned).sortedByDescending { it.stamp }
    }

    /** The one thing kept under a stamp, a reading or a scan; null where neither is. */
    suspend fun kept(stamp: String): Kept? = withContext(Dispatchers.IO) {
        File(readings, "$stamp.json").takeIf { it.exists() }?.let(::readingIn)
            ?: File(scans, stamp).takeIf { File(it, "scan.json").exists() }?.let(::scanKeptIn)
    }

    private fun readingIn(file: File): Kept =
        Kept.Read(file.nameWithoutExtension, listOf(file, File(readings, "${file.nameWithoutExtension}.png")), kindIn(file))

    /* The list wants the count and the outcome, not the cards, so each card stays whatever JSON it is. */
    private fun scanKeptIn(folder: File): Kept {
        summaries[folder.name]?.let { return it }
        val scan = scanIn(folder)
        val kept = Kept.Scan(folder.name, folder.listFiles().orEmpty().sortedBy { it.name }, kindOf(scan?.kind), scan?.entries?.size, scan?.outcome?.let(::endedOf), scan?.detail)
        /* Kept only once the scan is closed: a file still being written does not read, and its journal still sits beside it. */
        if (scan != null && !Journal.heldBy(folder)) summaries[folder.name] = kept

        return kept
    }

    private fun scanIn(folder: File): ScanFile<JsonElement>? = resultOf { readScan(folder, JsonElement.serializer()) }.getOrNull()

    /** The kind a reading names, which is all the list wants of a file it does not otherwise open. */
    private fun kindIn(file: File): Kind? =
        kindOf(resultOf { json.parseToJsonElement(file.readText()).jsonObject["kind"]?.jsonPrimitive?.content }.getOrNull())

    private fun kindOf(id: String?): Kind? = Kind.entries.firstOrNull { it.id == id }
}
