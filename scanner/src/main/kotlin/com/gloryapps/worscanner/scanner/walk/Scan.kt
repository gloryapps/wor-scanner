package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.reading.ScannedGear
import kotlinx.serialization.Serializable

/** One piece as the walk found it: where it sat in the grid, what the reader made of its panel, and the rows read. */
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

/** Why a walk ended. */
sealed interface Outcome {
    data class Finished(override val entries: List<ScanEntry>) : Outcome

    data class Stopped(val reason: Reason, override val entries: List<ScanEntry>) : Outcome

    data class Failed(val cause: Throwable, override val entries: List<ScanEntry>) : Outcome

    enum class Reason { STORAGE_NOT_OPEN, GRID_LOST, CANCELLED }

    val entries: List<ScanEntry>
}

data class Progress(val done: Int, val total: Int)
