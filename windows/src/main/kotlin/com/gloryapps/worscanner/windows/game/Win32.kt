package com.gloryapps.worscanner.windows.game

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.win32.W32APIOptions
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/** user32 as the scanner calls it: jna-platform's mapping and the few calls it leaves out. */
internal interface UserCalls : User32 {
    fun ClientToScreen(window: HWND, point: POINT): Boolean

    fun IsIconic(window: HWND): Boolean

    fun SetThreadDpiAwarenessContext(context: Pointer): Pointer?

    companion object {
        val user: UserCalls by lazy { Native.load("user32", UserCalls::class.java, W32APIOptions.DEFAULT_OPTIONS) }
    }
}

/** `DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2`: positions in the physical pixels of whichever monitor they fall on. */
private val PER_MONITOR_AWARE = Pointer(-4)

/**
 * The one thread every Win32 call runs on. It is aware of each monitor's scale, so a window's place,
 * a captured frame and a click all count the same pixels whatever the display's scaling.
 */
internal val Win32Thread = Executors.newSingleThreadExecutor { work ->
    Thread({ UserCalls.user.SetThreadDpiAwarenessContext(PER_MONITOR_AWARE); work.run() }, "wor-scanner-win32").apply { isDaemon = true }
}.asCoroutineDispatcher()
