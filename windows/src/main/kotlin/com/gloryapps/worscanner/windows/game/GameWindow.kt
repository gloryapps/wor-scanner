package com.gloryapps.worscanner.windows.game

import com.gloryapps.worscanner.scanner.text.Box
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC
import kotlinx.coroutines.delay
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

    /** Brings the window in front, which the click that asked lets the app do; false where Windows refused. */
    suspend fun bringToFront(handle: Long): Boolean = withContext(Win32Thread) { UserCalls.user.SetForegroundWindow(HWND(Pointer(handle))) }

    suspend fun inFront(handle: Long): Boolean = withContext(Win32Thread) { Pointer.nativeValue(UserCalls.user.GetForegroundWindow()?.pointer) == handle }

    /** Returns once the window is in front: while another is, the scan waits for the player to bring the game back. */
    suspend fun awaitFront(handle: Long) {
        while (!inFront(handle)) delay(LOOK_AGAIN_MS)
    }

    /**
     * Makes one of the app's windows a sign over the game: a click on it never takes the front from the
     * game, and no capture holds it, so it is never in a frame the scan reads.
     */
    suspend fun overGame(handle: Long) = withContext(Win32Thread) {
        val window = HWND(Pointer(handle))
        val user = UserCalls.user
        user.SetWindowLong(window, WinUser.GWL_EXSTYLE, user.GetWindowLong(window, WinUser.GWL_EXSTYLE) or NO_ACTIVATE)
        user.SetWindowDisplayAffinity(window, EXCLUDE_FROM_CAPTURE)
    }

    /** The window's client area now, in screen pixels; null once it is closed or minimised. */
    suspend fun clientOf(handle: Long): Box? = withContext(Win32Thread) {
        HWND(Pointer(handle)).takeIf { UserCalls.user.IsWindow(it) }?.let(::topWindow)?.takeIf { it.shown }?.client
    }

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
    private const val LOOK_AGAIN_MS = 250L
    private const val NO_ACTIVATE = 0x08000000
    private const val EXCLUDE_FROM_CAPTURE = 0x00000011
}
