package com.gloryapps.worscanner.windows.game

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A top-level window as the desktop lists it: `kind` is its window class, `area` how many pixels it draws in, none while minimised. */
internal data class TopWindow(val handle: Long, val title: String, val kind: String, val shown: Boolean, val area: Long)

/**
 * The game's window among the desktop's: shown, minimised or not, since a scan reads the game's memory
 * and not its face; a Unity player's, whatever its title, since each client of the game names it its
 * own way. Of several, the one naming the game, then the largest.
 */
internal fun gameAmong(windows: List<TopWindow>): TopWindow? =
    windows.filter { it.shown && it.kind == UNITY }.maxWithOrNull(compareBy({ it.title.contains(GAME, ignoreCase = true) }, { it.area }))

private const val GAME = "Watcher of Realms"
private const val UNITY = "UnityWndClass"

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
        val title = CharArray(TEXT_LENGTH).let { String(it, 0, user.GetWindowText(window, it, it.size)) }
        val kind = CharArray(TEXT_LENGTH).let { String(it, 0, user.GetClassName(window, it, it.size)) }
        val size = RECT().also { user.GetClientRect(window, it) }

        return TopWindow(
            handle = Pointer.nativeValue(window.pointer),
            title = title,
            kind = kind,
            shown = user.IsWindowVisible(window),
            area = size.right.toLong() * size.bottom,
        )
    }

    private const val TEXT_LENGTH = 256
}
