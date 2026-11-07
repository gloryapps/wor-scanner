package com.gloryapps.worscanner.windows.debug

import com.gloryapps.worscanner.scanner.account.Squads
import java.io.File

/** A dev build's Debug window: `squads` are those the game's open squad screen held when last read, none before a read, and `kept` the file they are kept in; `said` is why the last read failed. */
internal data class DebugState(
    val squads: List<Squads>? = null,
    val kept: File? = null,
    val reading: Boolean = false,
    val said: String? = null,
)

internal sealed interface DebugEvent {
    /** Reads the squads of the squad screen open in the game, each hero with what he wears. */
    data object ReadSquads : DebugEvent

    data object OpenKept : DebugEvent
}

internal sealed interface DebugEffect {
    data class Open(val folder: File) : DebugEffect
}
