package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.reading.ScannedGear
import kotlinx.serialization.Serializable

/**
 * One piece as the walk found it: its place in the walk, what the reader made of its panel, and
 * the rows read. `row` and `column` count from the tile the walk began on, which is the selected
 * one or the first in view.
 */
@Serializable
data class ScanEntry(
    val index: Int,
    val row: Int,
    val column: Int,
    val card: ScannedGear,
    val rows: List<String>,
    /** The panel kept as an image, only where the reader did not close the card. */
    val png: String? = null,
) {
    val unread: Boolean get() = card.set == null || card.slot == null
}

/** Why a walk ended; `seen` is what the reader made of the frame it ended on, for whoever asks why. */
sealed interface Outcome {
    data class Finished(override val entries: List<ScanEntry>, val seen: List<String> = emptyList()) : Outcome

    data class Stopped(val reason: Reason, override val entries: List<ScanEntry>, val detail: String, val seen: List<String> = emptyList()) : Outcome

    data class Failed(val cause: Throwable, override val entries: List<ScanEntry>) : Outcome

    enum class Reason { STORAGE_NOT_OPEN, GRID_LOST, CANCELLED }

    val entries: List<ScanEntry>
}

/** How many pieces are read, and how many the header says the storage holds. */
data class Progress(val done: Int, val held: Int)
