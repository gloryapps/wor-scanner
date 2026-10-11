package com.gloryapps.worscanner.windows.game

import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * The game's window as the desktop shows it, looked for again every second for as long as the app
 * runs: the player may open, move or close it at any time, and a scan takes it as it stands. A look
 * that fails counts as no game, and the next looks again.
 */
internal class GameWatch(scope: CoroutineScope) {
    val game: StateFlow<TopWindow?> = flow {
        while (Desktop.present) {
            emit(resultOf { Desktop.game() }.getOrNull())
            delay(EVERY_MS)
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private companion object {
        const val EVERY_MS = 1_000L
    }
}
