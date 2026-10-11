package com.gloryapps.worscanner.windows.game

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A top-level window as the desktop lists it: `area` is how many pixels it draws in, none while minimised. */
internal data class TopWindow(val handle: Long, val title: String, val shown: Boolean, val area: Long)

/**
 * The game's window among the desktop's: shown, minimised or not, since a scan reads the game's memory
 * and not its face; titled exactly as the game, the largest where several are. Exactly, since a
 * browser on the wiki holds the game's name in a longer title.
 */
internal fun gameAmong(windows: List<TopWindow>): TopWindow? =
    windows.filter { it.shown && it.title.trim().equals(GAME, ignoreCase = true) }.maxByOrNull { it.area }

private const val GAME = "Watcher of Realms"

/** The desktop's windows; only Windows has one. */
internal object Desktop {
    val present: Boolean = System.getProperty("os.name").startsWith("Windows")

    suspend fun game(): TopWindow? = gameAmong(windows())

    private suspend fun windows(): List<TopWindow> = withContext(Dispatchers.IO) {
        buildList {
            User32.INSTANCE.EnumWindows(WNDENUMPROC { window, _ -> add(topWindow(window)); true }, null)
        }
    }

    private fun topWindow(window: HWND): TopWindow {
        val user = User32.INSTANCE
        val title = CharArray(TITLE_LENGTH).let { String(it, 0, user.GetWindowText(window, it, it.size)) }
        val size = RECT().also { user.GetClientRect(window, it) }

        return TopWindow(
            handle = Pointer.nativeValue(window.pointer),
            title = title,
            shown = user.IsWindowVisible(window),
            area = size.right.toLong() * size.bottom,
        )
    }

    private const val TITLE_LENGTH = 256
}
