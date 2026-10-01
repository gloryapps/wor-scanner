package com.gloryapps.worscanner.ui.home

import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.update.Update
import java.io.File

internal data class HomeUiState(
    /** What the system lets the app do now. */
    val granted: Set<Permission> = emptySet(),
    /** Start was pressed with a grant off: what is missing is asked over the screen until the scan starts or the ask is dismissed. */
    val asking: Boolean = false,
    val capturing: Boolean = false,
    /** The scan under way, for the header; null when none is running. */
    val running: ScanState.Running? = null,
    val update: Update = Update.None,
) {
    /** A scan can only start once the system lets the app both draw over the game and touch it. */
    val ready: Boolean get() = Permission.entries.all { !it.needed || it in granted }

    /** The grants still off, in the order they are asked for. */
    val missing: List<Permission> get() = Permission.entries.filterNot { it in granted }
}

internal sealed interface HomeEvent {

    data class Grant(val permission: Permission) : HomeEvent

    /** Ready's Start: the scan starts at once when every grant is on, and asks for the missing ones when not. */
    data object Start : HomeEvent

    /** The ask's Start, which waits for the grants a scan needs and goes on without the others. */
    data object Begin : HomeEvent

    data object Dismiss : HomeEvent

    data object Stop : HomeEvent

    /** The header's way to every scan kept on the device. */
    data object Earlier : HomeEvent

    /** The header's update link: the newer release installed, or its page opened where its APK could not be had. */
    data object Update : HomeEvent
}

/** What the screen does once, on the system's or the stack's side, when the ViewModel says so. */
internal sealed interface HomeEffect {

    /** The system's own way to give a grant: its settings screen, or its dialog. */
    data class Grant(val permission: Permission) : HomeEffect

    data object LaunchProjection : HomeEffect

    data object StopCapture : HomeEffect

    data object OpenEarlier : HomeEffect

    data class Install(val apk: File) : HomeEffect

    data class OpenPage(val url: String) : HomeEffect
}
