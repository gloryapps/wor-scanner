package com.gloryapps.worscanner.ui.earlier

import com.gloryapps.worscanner.scanner.runs.Kept

internal data class EarlierUiState(
    /** Null until the store has answered, so a start does not show an empty list first. */
    val readings: List<Kept>? = null,
    /** The readings whose deletion is being asked about, one or every one; empty while none is. */
    val deleting: List<Kept> = emptyList(),
    /** Whether the scanner holds a token from Azhor's Master Smithy, which only a send from Home gives it. */
    val linked: Boolean = false,
)

internal sealed interface EarlierEvent {

    data object Back : EarlierEvent

    data class Open(val kept: Kept) : EarlierEvent

    data class Export(val kept: Kept) : EarlierEvent

    data object ExportAll : EarlierEvent

    data class Delete(val kept: Kept) : EarlierEvent

    data object DeleteAll : EarlierEvent

    data object ConfirmDelete : EarlierEvent

    data object CancelDelete : EarlierEvent

    data object Unlink : EarlierEvent
}

/** What the screen does once, on the stack's side, when the ViewModel says so. */
internal sealed interface EarlierEffect {

    data object NavigateBack : EarlierEffect

    data class OpenReading(val kept: Kept) : EarlierEffect
}
