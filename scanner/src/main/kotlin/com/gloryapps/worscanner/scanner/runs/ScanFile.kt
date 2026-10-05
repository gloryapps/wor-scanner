package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

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

fun <T> writeScan(folder: File, scan: ScanFile<T>, card: KSerializer<T>) = File(folder, SCAN).writeText(JSON.encodeToString(ScanFile.serializer(card), scan))

fun <T> readScan(folder: File, card: KSerializer<T>): ScanFile<T> = JSON.decodeFromString(ScanFile.serializer(card), File(folder, SCAN).readText())

private const val SCAN = "scan.json"
private val JSON = Json { prettyPrint = true; ignoreUnknownKeys = true }
