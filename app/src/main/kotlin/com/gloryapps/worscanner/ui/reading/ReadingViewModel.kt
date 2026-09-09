package com.gloryapps.worscanner.ui.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.capture.Readings
import com.gloryapps.worscanner.ui.ExportDelegate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal class ReadingViewModel(stamp: String, readings: Readings, val export: ExportDelegate) : ViewModel() {
    private val _state = MutableStateFlow(read(stamp, readings))
    val state: StateFlow<ReadingUiState> = _state.asStateFlow()

    private val _effects = Channel<ReadingEffect>(Channel.BUFFERED)
    val effects: Flow<ReadingEffect> = _effects.receiveAsFlow()

    fun on(event: ReadingEvent) {
        when (event) {
            ReadingEvent.Back -> send(ReadingEffect.NavigateBack)
            ReadingEvent.Close -> _state.update { it.copy(opened = false) }
            is ReadingEvent.Choose -> _state.update { it.copy(chosen = event.at, showing = Showing.PIECE, opened = true) }
            is ReadingEvent.Show -> _state.update { it.copy(showing = event.showing) }
            ReadingEvent.Export -> _state.value.kept?.let(export::begin)
            is ReadingEvent.Copy -> send(ReadingEffect.Copy(event.label, event.text))
        }
    }

    private fun send(effect: ReadingEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun read(stamp: String, readings: Readings): ReadingUiState {
        val kept = readings.list().firstOrNull { it.stamp == stamp } ?: return ReadingUiState()
        val scan = readings.opened(kept)

        return ReadingUiState(
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
