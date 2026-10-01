package com.gloryapps.worscanner.ui.earlier

import androidx.annotation.StringRes
import com.gloryapps.worscanner.capture.Kept

internal data class EarlierUiState(
    /** Null until the store has answered, so a start does not show an empty list first. */
    val readings: List<Kept>? = null,
    /** The readings whose deletion is being asked about, one or every one; empty while none is. */
    val deleting: List<Kept> = emptyList(),
    val site: SiteLink = SiteLink(),
)

/** The scanner's link to Azhor's Master Smithy: whether it holds a token, whether a code is on its way, and why the last one did not link. */
internal data class SiteLink(val linked: Boolean = false, val asking: Boolean = false, @StringRes val refused: Int? = null)

internal sealed interface EarlierEvent {

    data object Back : EarlierEvent

    data class Open(val kept: Kept) : EarlierEvent

    data class Export(val kept: Kept) : EarlierEvent

    data object ExportAll : EarlierEvent

    data class Delete(val kept: Kept) : EarlierEvent

    data object DeleteAll : EarlierEvent

    data object ConfirmDelete : EarlierEvent

    data object CancelDelete : EarlierEvent

    /** A code the site showed, typed into the scanner to link it. */
    data class Link(val code: String) : EarlierEvent

    data object Unlink : EarlierEvent
}

/** What the screen does once, on the stack's side, when the ViewModel says so. */
internal sealed interface EarlierEffect {

    data object NavigateBack : EarlierEffect

    data class OpenReading(val kept: Kept) : EarlierEffect
}
