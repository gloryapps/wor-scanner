package com.gloryapps.worscanner.windows.home

import com.gloryapps.worscanner.scanner.azhor.Send
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.runs.Kept
import com.gloryapps.worscanner.scanner.text.Box
import java.io.File

/**
 * `over` is where the game draws while a scan runs, which the sign over it stands on, and `progress`
 * what the scan says of itself there. `said` is what the last Read or scan came to, `kept` its folder.
 * `scan` is the newest scan kept, to be sent to the lab, and `send` how sending it stands.
 */
internal data class HomeState(
    val game: Game = Game.Looking,
    val kind: Kind = Kind.GEAR,
    val reading: Boolean = false,
    val over: Box? = null,
    val progress: String? = null,
    /** The scan waits for the game to be back in front. */
    val paused: Boolean = false,
    val said: String? = null,
    val kept: File? = null,
    val scan: Kept.Scan? = null,
    val send: Send = Send.Idle,
) {
    val scanning: Boolean get() = over != null
}

/** The game's window as the home screen says it. */
internal sealed interface Game {
    data object Looking : Game

    /** `width` by `height` is the area the game draws in, in the display's pixels. */
    data class Open(val width: Int, val height: Int) : Game

    data object Closed : Game

    /** The app runs on a system with no Windows desktop to find the game on: a developer's Mac. */
    data object NotWindows : Game
}

internal sealed interface HomeEvent {
    data class Choose(val kind: Kind) : HomeEvent

    data object Read : HomeEvent

    /** Scans the chosen kind, or stops the scan under way. */
    data object Scan : HomeEvent

    data object OpenKept : HomeEvent

    data object Send : HomeEvent

    data class LinkAndSend(val code: String) : HomeEvent

    /** The scan is put away, sent or not: the next one is to come. */
    data object Next : HomeEvent
}

internal sealed interface HomeEffect {
    /** The app's window comes back in front, the game having been brought there to be read. */
    data object Front : HomeEffect

    data class Open(val folder: File) : HomeEffect
}
