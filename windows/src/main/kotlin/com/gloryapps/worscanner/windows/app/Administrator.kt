package com.gloryapps.worscanner.windows.app

import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.WinUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The scanner started again as administrator, for a game that runs as one. */
internal object Administrator {
    /** Starts another scanner as administrator once the player says yes to Windows' prompt; false where they say no. */
    suspend fun restart(): Boolean = withContext(Dispatchers.IO) {
        val app = ProcessHandle.current().info().command().orElse(null) ?: return@withContext false

        Shell32.INSTANCE.ShellExecute(null, "runas", app, null, null, WinUser.SW_SHOWNORMAL).toLong() > SHELL_ERRORS
    }

    /** `ShellExecute` answers a number up to 32 where it started nothing. */
    private const val SHELL_ERRORS = 32
}
