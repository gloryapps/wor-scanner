package com.gloryapps.worscanner.windows.hand

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import java.util.concurrent.CountDownLatch

/**
 * Esc, taken from every window while it is held, `onPress` called each time it is pressed: how the
 * player stops a scan with the game in front. Windows sends a hot key to the thread that took it, so
 * the key is held by a thread of its own, waiting on its messages until closed.
 */
internal class EscapeKey(private val onPress: () -> Unit) : AutoCloseable {
    @Volatile private var thread = 0

    init {
        val held = CountDownLatch(1)
        Thread({
            val user = User32.INSTANCE
            thread = Kernel32.INSTANCE.GetCurrentThreadId()
            val taken = user.RegisterHotKey(null, ID, WinUser.MOD_NOREPEAT, ESCAPE)
            held.countDown()
            if (taken) {
                val message = WinUser.MSG()
                while (user.GetMessage(message, null, 0, 0) > 0) {
                    if (message.message == WinUser.WM_HOTKEY) onPress()
                }
                user.UnregisterHotKey(null, ID)
            }
        }, "wor-scanner-escape").apply { isDaemon = true }.start()
        held.await()
    }

    override fun close() {
        User32.INSTANCE.PostThreadMessage(thread, WinUser.WM_QUIT, WPARAM(0), LPARAM(0))
    }

    private companion object {
        const val ID = 1
        const val ESCAPE = 0x1B
    }
}
