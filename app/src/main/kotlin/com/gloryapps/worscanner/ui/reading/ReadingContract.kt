package com.gloryapps.worscanner.ui.reading

import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.outgoingName

/** One tile of a scan as the screen shows it: what the reader saw, and what it made of it. */
internal data class Piece(
    val index: Int,
    val row: Int,
    val column: Int,
    /** The first row of the panel, which is what the tile is called. */
    val name: String,
    /** The rest of the rows, in one line. */
    val said: String,
    val card: String,
    /** False where the reader kept the panel as an image because it could not close the card. */
    val closed: Boolean,
)

/** Which of the two the screen is showing: the piece the reader picked, or the file as written. */
internal enum class Showing { PIECE, FILE }

internal data class ReadingUiState(
    val kept: Kept? = null,
    val kind: Kind? = null,
    val pieces: List<Piece> = emptyList(),
    val file: String = "",
    val chosen: Int = 0,
    val showing: Showing = Showing.PIECE,
    /** Where the screen is too narrow for both, whether the piece has taken the list's place. */
    val opened: Boolean = false,
) {
    val piece: Piece? get() = pieces.getOrNull(chosen)

    /** How many tiles the reader could not close, which is what the foot of the list counts. */
    val open: Int get() = pieces.count { !it.closed }

    /** The name the file leaves under, which is what the screen calls it. */
    val name: String get() = kept?.outgoingName().orEmpty()
}

internal sealed interface ReadingEvent {

    data object Back : ReadingEvent

    /** Leave the piece for the list it was chosen from. */
    data object Close : ReadingEvent

    data class Choose(val at: Int) : ReadingEvent

    data class Show(val showing: Showing) : ReadingEvent

    data object Export : ReadingEvent

    data class Copy(val label: String, val text: String) : ReadingEvent
}

internal sealed interface ReadingEffect {

    data object NavigateBack : ReadingEffect

    data class Copy(val label: String, val text: String) : ReadingEffect
}
