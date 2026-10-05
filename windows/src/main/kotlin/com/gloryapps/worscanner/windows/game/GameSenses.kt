package com.gloryapps.worscanner.windows.game

import com.gloryapps.worscanner.scanner.senses.Senses
import com.gloryapps.worscanner.windows.capture.DesktopScreen
import com.gloryapps.worscanner.windows.hand.MouseHand

/** The eyes and the hand over the game's window as the watch last saw it. */
internal class GameSenses(private val watch: GameWatch) : Senses {
    override val screen get() = DesktopScreen(game())
    override val hand get() = MouseHand(game())

    private fun game(): Long = checkNotNull(watch.game.value) { "Watcher of Realms is not open" }.handle
}
