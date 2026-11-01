package com.gloryapps.worscanner.windows.memory

import com.gloryapps.worscanner.scanner.client.InstalledClient
import com.sun.jna.platform.win32.KnownFolders
import com.sun.jna.platform.win32.Shell32Util
import java.io.File

/** The client the game's process runs, where a Unity player keeps it on Windows: its data beside its exe, and the folder its company and product name in LocalLow, as its data's `app.info` gives them. */
internal suspend fun GameMemory.client(): InstalledClient {
    val install = exe.parentFile
    val data = File(install, "${exe.nameWithoutExtension}_Data")
    val (company, product) = File(data, "app.info").readLines()
    val saved = File(File(Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_LocalAppDataLow), company), product)

    return InstalledClient.read(this, install, data, saved)
}
