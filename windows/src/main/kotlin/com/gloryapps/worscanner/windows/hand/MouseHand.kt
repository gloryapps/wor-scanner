package com.gloryapps.worscanner.windows.hand

import com.gloryapps.worscanner.scanner.senses.Touch
import com.gloryapps.worscanner.windows.game.Desktop
import com.gloryapps.worscanner.windows.game.UserCalls
import com.gloryapps.worscanner.windows.game.Win32Thread
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinUser.INPUT
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** A place on the screen, in physical pixels. */
internal data class ScreenPoint(val x: Int, val y: Int)

/** The left button, pressed or let go. */
internal enum class Button(val flag: Int) { DOWN(0x0002), UP(0x0004) }

/** The left button pressed or let go wherever the cursor stands, as the mouse itself would. */
internal fun press(button: Button) {
    val input = INPUT().apply {
        type = DWORD(INPUT.INPUT_MOUSE.toLong())
        input.setType("mi")
        input.mi.dwFlags = DWORD(button.flag.toLong())
    }
    check(UserCalls.user.SendInput(DWORD(1), arrayOf(input), input.size()).toInt() == 1) { "the click was refused" }
}

/**
 * The hand over the game's window: the cursor put where the frame says, and the left button pressed
 * and let go. A drag moves, holds still, then lets go, so the grid takes no fling from the release.
 * Each move waits for the game to be in front, where the click lands: the player may use another
 * window meanwhile, and the scan goes on when the game is back. The mouse may wander over the game
 * too; the hand puts the cursor back where it taps.
 */
internal class MouseHand(private val handle: Long) : Touch {
    override suspend fun tap(x: Int, y: Int) = withContext(Win32Thread) {
        moveTo(onScreen(x, y))
        press(Button.DOWN)
        delay(TAP_MS)
        press(Button.UP)
    }

    override suspend fun drag(fromX: Int, fromY: Int, toX: Int, toY: Int, millis: Long) = withContext(Win32Thread) {
        val from = onScreen(fromX, fromY)
        val to = onScreen(toX, toY)
        moveTo(from)
        press(Button.DOWN)
        try {
            val steps = (millis / STEP_MS).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                delay(millis / steps)
                moveTo(ScreenPoint(from.x + (to.x - from.x) * step / steps, from.y + (to.y - from.y) * step / steps))
            }
            delay(HOLD_MS)
        } finally {
            press(Button.UP)
        }
    }

    /** A place on the frame, which is the game's client area, as a place on the screen now. */
    private suspend fun onScreen(x: Int, y: Int): ScreenPoint {
        val client = checkNotNull(Desktop.clientOf(handle)) { "the game's window is closed or minimised" }

        return ScreenPoint(client.left + x, client.top + y)
    }

    private suspend fun moveTo(point: ScreenPoint) {
        Desktop.awaitFront(handle)
        check(UserCalls.user.SetCursorPos(point.x.toLong(), point.y.toLong())) { "the cursor could not be moved" }
    }

    private companion object {
        const val TAP_MS = 60L
        const val HOLD_MS = 200L
        const val STEP_MS = 10L
    }
}
