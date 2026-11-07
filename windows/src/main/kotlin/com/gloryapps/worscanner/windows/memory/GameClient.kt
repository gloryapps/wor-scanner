package com.gloryapps.worscanner.windows.memory

import com.gloryapps.worscanner.scanner.client.Client
import com.gloryapps.worscanner.scanner.client.InstalledClient
import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.scanner.runs.Reports
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.account_elevated
import com.gloryapps.worscanner.windows.resources.account_refused
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
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

/** The client, or none where it cannot be read, reported: without it the account's heroes are no cards, and its gear and artifacts still are. */
internal suspend fun GameMemory.clientOrNone(reports: Reports): Client? =
    resultOf { client() }.onFailure { reports.failed("reading the game's client", it) }.getOrNull()

/** Why a read of the game's memory failed, as the player is told: Windows's refusals in their own words, anything else as `failed` with its message. */
internal suspend fun Throwable.said(failed: StringResource): String = when (this) {
    is GameMemory.Elevated -> getString(Res.string.account_elevated)
    is GameMemory.Refused -> getString(Res.string.account_refused, code)
    else -> getString(failed, message ?: toString())
}
