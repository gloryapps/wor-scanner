package com.gloryapps.worscanner.windows.game

import com.gloryapps.worscanner.scanner.text.Box
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import kotlinx.coroutines.withContext

/** A top-level window as the desktop lists it: `client` is the area it draws in, in screen pixels. */
internal data class TopWindow(val handle: Long, val title: String, val shown: Boolean, val client: Box)

/**
 * The game's window among the desktop's: shown, not minimised, titled exactly as the game, the largest
 * where several are. Exactly, since a browser on the wiki holds the game's name in a longer title.
 */
internal fun gameAmong(windows: List<TopWindow>): TopWindow? =
    windows.filter { it.shown && it.title.trim().equals(GAME, ignoreCase = true) }.maxByOrNull { it.client.width.toLong() * it.client.height }

private const val GAME = "Watcher of Realms"

/** The desktop's windows, read on the Win32 thread; only Windows has one. */
internal object Desktop {
    val present: Boolean = System.getProperty("os.name").startsWith("Windows")

    suspend fun game(): TopWindow? = gameAmong(windows())

    private suspend fun windows(): List<TopWindow> = withContext(Win32Thread) {
        buildList {
            UserCalls.user.EnumWindows(WNDENUMPROC { window, _ -> add(topWindow(window)); true }, null)
        }
    }

    private fun topWindow(window: HWND): TopWindow {
        val user = UserCalls.user
        val title = CharArray(TITLE_LENGTH).let { String(it, 0, user.GetWindowText(window, it, it.size)) }
        val size = RECT().also { user.GetClientRect(window, it) }
        val origin = POINT(0, 0).also { user.ClientToScreen(window, it) }

        return TopWindow(
            handle = Pointer.nativeValue(window.pointer),
            title = title,
            shown = user.IsWindowVisible(window) && !user.IsIconic(window),
            client = Box(origin.x, origin.y, origin.x + size.right, origin.y + size.bottom),
        )
    }

    private const val TITLE_LENGTH = 256
}
