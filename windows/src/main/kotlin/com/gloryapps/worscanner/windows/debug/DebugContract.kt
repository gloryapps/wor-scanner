package com.gloryapps.worscanner.windows.debug

import com.gloryapps.worscanner.scanner.account.Play
import com.gloryapps.worscanner.scanner.account.Squads
import com.gloryapps.worscanner.scanner.simulator.Rules
import java.io.File

/**
 * A dev build's Debug window: `squads` are those the game's open squad screen held when last read, none before a read, and `kept` the
 * file they are kept in; `rules` the ways the Armory's simulator lets them be played, none where it did not answer the read; `play` is how
 * the player says he plays them, `saved` whether the file holds it as it stands, `placing` the hero a tap on the map puts; `said` is why the
 * last read failed.
 */
internal data class DebugState(
    val squads: List<Squads>? = null,
    val kept: File? = null,
    val rules: Rules? = null,
    val play: Play = Play(),
    val saved: Boolean = false,
    val placing: String? = null,
    val reading: Boolean = false,
    val said: String? = null,
)

/** A hero is named as [com.gloryapps.worscanner.scanner.account.called] names him, a way to play him as the simulator's rules do. */
internal sealed interface DebugEvent {
    /** Reads the squads of the squad screen open in the game, each hero with what he wears, and asks the simulator how they may be played. */
    data object ReadSquads : DebugEvent

    /** Deploys a hero after those deployed so far, or takes him out of the order. */
    data class Deploy(val hero: String) : DebugEvent

    data class Healer(val hero: String) : DebugEvent

    data class Inspiration(val hero: String) : DebugEvent

    data class Stance(val hero: String, val stance: String) : DebugEvent

    data class Ultimate(val hero: String, val way: String) : DebugEvent

    data class Tick(val hero: String, val tick: String) : DebugEvent

    /** Picks the hero the next tap on the map puts, or lets him go. */
    data class Place(val hero: String) : DebugEvent

    /** Puts the hero being placed on a cell of the map, or takes him off his own; with none being placed, picks the hero on it. */
    data class PutOn(val cell: List<Double>) : DebugEvent

    data class Face(val hero: String, val face: Int) : DebugEvent

    /** Keeps the squads again, with the play said of them. */
    data object Save : DebugEvent

    data object OpenKept : DebugEvent
}

internal sealed interface DebugEffect {
    data class Open(val folder: File) : DebugEffect
}
