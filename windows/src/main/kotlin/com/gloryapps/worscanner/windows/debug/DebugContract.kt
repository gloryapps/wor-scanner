package com.gloryapps.worscanner.windows.debug

import com.gloryapps.worscanner.scanner.account.Play
import com.gloryapps.worscanner.scanner.account.Squads
import java.io.File

/**
 * A dev build's Debug window: `squads` are those the game's open squad screen held when last read, none before a read, and `kept` the
 * file they are kept in; `play` is how the player says he plays them, `saved` whether the file holds it as it stands; `said` is why the
 * last read failed.
 */
internal data class DebugState(
    val squads: List<Squads>? = null,
    val kept: File? = null,
    val play: Play = Play(),
    val saved: Boolean = false,
    val reading: Boolean = false,
    val said: String? = null,
)

/** A hero is named as [com.gloryapps.worscanner.scanner.account.called] names him. */
internal sealed interface DebugEvent {
    /** Reads the squads of the squad screen open in the game, each hero with what he wears. */
    data object ReadSquads : DebugEvent

    /** Deploys a hero after those deployed so far, or takes him out of the order. */
    data class Deploy(val hero: String) : DebugEvent

    data class Healer(val hero: String) : DebugEvent

    data class Inspiration(val hero: String) : DebugEvent

    data class Stance(val hero: String, val words: String) : DebugEvent

    data class Ultimate(val hero: String, val words: String) : DebugEvent

    /** Keeps the squads again, with the play said of them. */
    data object Save : DebugEvent

    data object OpenKept : DebugEvent
}

internal sealed interface DebugEffect {
    data class Open(val folder: File) : DebugEffect
}
