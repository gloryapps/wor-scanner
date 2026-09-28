package com.gloryapps.worscanner.ui.home

import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.scan.Chosen
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.update.Update
import java.io.File

internal data class HomeUiState(
    val accessibilityOn: Boolean = false,
    val overlayAllowed: Boolean = false,
    val capturing: Boolean = false,
    val kind: Kind = Chosen.FIRST,
    /** The scan under way, for the header; null when none is running. */
    val running: ScanState.Running? = null,
    /** Null until the store has answered, so a start does not show an empty list first. */
    val readings: List<Kept>? = null,
    /** The readings whose deletion is being asked about, one or every one; empty while none is. */
    val deleting: List<Kept> = emptyList(),
    val update: Update = Update.None,
) {
    /** A scan can only start once the system lets the app both see the screen and touch it. */
    val ready: Boolean get() = accessibilityOn && overlayAllowed
}

internal sealed interface HomeEvent {

    data object GrantAccessibility : HomeEvent

    data object GrantOverlay : HomeEvent

    data object Start : HomeEvent

    data object Stop : HomeEvent

    data class Choose(val kind: Kind) : HomeEvent

    data class Open(val kept: Kept) : HomeEvent

    data class Export(val kept: Kept) : HomeEvent

    data object ExportAll : HomeEvent

    data class Delete(val kept: Kept) : HomeEvent

    data object DeleteAll : HomeEvent

    data object ConfirmDelete : HomeEvent

    data object CancelDelete : HomeEvent

    /** The header's update link: the newer release installed, or its page opened where its APK could not be had. */
    data object Update : HomeEvent
}

/** What the screen does once, on the system's or the stack's side, when the ViewModel says so. */
internal sealed interface HomeEffect {

    data object OpenAccessibilitySettings : HomeEffect

    data object OpenOverlaySettings : HomeEffect

    data object LaunchProjection : HomeEffect

    data object StopCapture : HomeEffect

    data class OpenReading(val kept: Kept) : HomeEffect

    data class Install(val apk: File) : HomeEffect

    data class OpenPage(val url: String) : HomeEffect
}
