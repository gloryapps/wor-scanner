package com.gloryapps.worscanner.ui.reading

import androidx.lifecycle.ViewModel
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Readings
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.Exporting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** One tile of a scan as the screen shows it: what the reader saw, and what it made of it. */
data class Piece(
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
enum class Showing { PIECE, FILE }

data class ReadingState(
    val kept: Kept? = null,
    val kind: Kind? = null,
    val pieces: List<Piece> = emptyList(),
    val file: String = "",
    val chosen: Int = 0,
    val showing: Showing = Showing.PIECE,
) {
    val piece: Piece? get() = pieces.getOrNull(chosen)

    /** How many tiles the reader could not close, which is what the foot of the list counts. */
    val open: Int get() = pieces.count { !it.closed }
}

class ReadingViewModel(stamp: String, readings: Readings, val exporting: Exporting) : ViewModel() {
    private val _state = MutableStateFlow(read(stamp, readings))
    val state: StateFlow<ReadingState> = _state.asStateFlow()

    fun choose(at: Int) {
        _state.value = _state.value.copy(chosen = at, showing = Showing.PIECE)
    }

    fun show(showing: Showing) {
        _state.value = _state.value.copy(showing = showing)
    }

    private fun read(stamp: String, readings: Readings): ReadingState {
        val kept = readings.list().firstOrNull { it.stamp == stamp } ?: return ReadingState()
        val scan = readings.opened(kept)

        return ReadingState(
            kept = kept,
            kind = kept.kind,
            pieces = scan?.entries.orEmpty().map { entry ->
                Piece(
                    index = entry.index,
                    row = entry.row,
                    column = entry.column,
                    name = entry.rows.firstOrNull().orEmpty(),
                    said = entry.rows.drop(1).joinToString(" · "),
                    card = PRETTY.encodeToString(JsonElement.serializer(), entry.card),
                    closed = entry.png == null,
                )
            },
            file = readings.text(kept),
            showing = if (scan == null) Showing.FILE else Showing.PIECE,
        )
    }

    private companion object {
        val PRETTY = Json { prettyPrint = true }
    }
}
