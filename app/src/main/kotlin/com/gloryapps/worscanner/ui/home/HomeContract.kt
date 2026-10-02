package com.gloryapps.worscanner.ui.home

import com.gloryapps.worscanner.azhor.Linking
import com.gloryapps.worscanner.azhor.Sending
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.SharedFolder
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.Permission
import com.gloryapps.worscanner.ui.canScan
import com.gloryapps.worscanner.update.Update
import kotlinx.serialization.Serializable
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
    /** The scan that just ended, which the screen shows in place of how to scan until it is put away. */
    val justScanned: JustScanned? = null,
) {
    val ready: Boolean get() = granted.canScan

    /** The grants still off, in the order they are asked for. */
    val missing: List<Permission> get() = Permission.entries.filterNot { it in granted }
}

/** A state Home opens in rather than reaches by playing: a debug build's way to see it as it runs. */
@Serializable
sealed interface Stage {

    /** The newest scan kept on the device, as if it had just ended. */
    @Serializable
    data object JustScanned : Stage

    /** The ask, as if Start had been pressed with a grant off. */
    @Serializable
    data object Asking : Stage
}

/** The newest scan as Home shows it once it ends: the scan, where its file can land, and how sending it went. */
internal data class JustScanned(
    val scan: Kept.Scan,
    val kind: Kind,
    /** It stopped at a touch of the screen, the player's own stop; any other stop is said by `scan.detail`. */
    val touched: Boolean,
    /** The emulator's folder Save lands in; null on a device without one. */
    val shared: SharedFolder?,
    val send: Send = Send.Idle,
)

/** Where sending the scan to the site stands. */
internal sealed interface Send {

    /** Nothing tried yet. */
    data object Idle : Send

    /** The link step, the code the site shows typed here; `failed` is why the last code did not link. */
    data class Code(val failed: Linking? = null) : Send

    data object Underway : Send

    data object Sent : Send

    /** The site did not take it, and said why: unlinked, too large, refused, or no answer. */
    data class Unsent(val why: Sending) : Send
}

internal sealed interface HomeEvent {

    data class Grant(val permission: Permission) : HomeEvent

    /** Ready's Start: the scan starts at once when every grant is on, and asks for the missing ones when not. */
    data object Start : HomeEvent

    /** The ask's Start, which waits for the grants a scan needs and goes on without the others. */
    data object Begin : HomeEvent

    data object Dismiss : HomeEvent

    data object Stop : HomeEvent

    /** The scan sent, or the link step opened when the scanner holds no token. */
    data object Send : HomeEvent

    /** A code the site showed, traded for a token, and the scan sent with it. */
    data class LinkAndSend(val code: String) : HomeEvent

    /** The scan's file saved into the emulator's folder. */
    data object Save : HomeEvent

    data object Share : HomeEvent

    /** Scan something else, or back over the scan: it is put away and the screen teaches again. */
    data object Next : HomeEvent

    /** The header's way to every scan kept on the device. */
    data object Earlier : HomeEvent

    /** The header's way to a debug build's screens. */
    data object Debug : HomeEvent

    /** The banner's update: the newer release installed, or its page opened where its APK could not be had. */
    data object Update : HomeEvent
}

/** What the screen does once, on the system's or the stack's side, when the ViewModel says so. */
internal sealed interface HomeEffect {

    /** The system's own way to give a grant: its settings screen, or its dialog. */
    data class Grant(val permission: Permission) : HomeEffect

    data object LaunchProjection : HomeEffect

    data object StopCapture : HomeEffect

    data object OpenEarlier : HomeEffect

    data object OpenDebug : HomeEffect

    data class Install(val apk: File) : HomeEffect

    data class OpenPage(val url: String) : HomeEffect
}
