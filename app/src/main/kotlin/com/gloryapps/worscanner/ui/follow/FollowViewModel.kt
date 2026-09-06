package com.gloryapps.worscanner.ui.follow

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.gloryapps.worscanner.capture.Exports
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Readings
import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FollowState(
    val readings: List<Kept> = emptyList(),
    /** What the last export said, shown once and cleared when the reader has seen it. */
    val said: String? = null,
)

class FollowViewModel(
    private val readings: Readings,
    private val exports: Exports,
) : ViewModel() {
    private val _state = MutableStateFlow(FollowState(readings.list()))
    val state: StateFlow<FollowState> = _state.asStateFlow()

    val downloadsNeedPermission: Boolean get() = exports.downloadsNeedPermission

    fun refresh() = _state.update { it.copy(readings = readings.list()) }

    fun saveToDownloads(kept: Kept) = save { exports.toDownloads(kept.files) }

    fun saveToPictures(kept: Kept) = save { exports.toPictures(kept.files) }

    private fun save(export: () -> List<String>) {
        val said = resultOf(export).fold(
            onSuccess = { paths -> "Saved to ${paths.first().substringBeforeLast('/')}" },
            onFailure = { "Saving failed: ${it.message}" },
        )
        _state.update { it.copy(said = said) }
    }

    fun shareIntent(kept: Kept): Intent = exports.shareIntent(kept.files)

    fun text(kept: Kept): String = readings.text(kept)

    fun delete(kept: Kept) {
        readings.delete(kept)
        refresh()
    }

    fun heard() = _state.update { it.copy(said = null) }

    private inline fun MutableStateFlow<FollowState>.update(change: (FollowState) -> FollowState) {
        value = change(value)
    }
}
