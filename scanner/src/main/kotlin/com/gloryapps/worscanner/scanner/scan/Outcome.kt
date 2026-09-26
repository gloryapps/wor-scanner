package com.gloryapps.worscanner.scanner.scan

import kotlinx.serialization.Serializable

/**
 * One tile as the scan found it: its place in the scan, what the reader made of its panel, and
 * the rows read. `row` counts from the row the scan began on, the selected tile's or the first in
 * view; `column` is the grid's own, from its left edge.
 */
@Serializable
data class ScanEntry<T>(
    val index: Int,
    val row: Int,
    val column: Int,
    val card: T,
    val rows: List<String>,
    /** The panel kept as an image, only where the reader did not close the card. */
    val png: String? = null,
)

/** Why a scan ended; `seen` is what the reader made of the frame it ended on, for whoever asks why. */
sealed interface Outcome<T> {
    data class Finished<T>(override val entries: List<ScanEntry<T>>, val seen: List<String> = emptyList()) : Outcome<T>

    data class Stopped<T>(val reason: Reason, override val entries: List<ScanEntry<T>>, val detail: String, val seen: List<String> = emptyList()) : Outcome<T>

    data class Failed<T>(val cause: Throwable, override val entries: List<ScanEntry<T>>) : Outcome<T>

    enum class Reason { STORAGE_NOT_OPEN, GRID_LOST, CANCELLED }

    val entries: List<ScanEntry<T>>
}

/** How many tiles are read, and how many the header says the screen holds. */
data class Progress(val done: Int, val held: Int)
