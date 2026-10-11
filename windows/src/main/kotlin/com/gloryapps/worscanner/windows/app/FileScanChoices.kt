package com.gloryapps.worscanner.windows.app

import com.gloryapps.worscanner.scanner.account.ScanChoices
import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** What the player chose a scan sends, in a file of its own in the app's folder; the scan's defaults until he chooses, or where the file will not read. */
internal class FileScanChoices(private val file: File) {
    private val kept = MutableStateFlow(read())
    val chosen: StateFlow<ScanChoices> = kept
    private val writing = Mutex()

    /** Stands at once, so a choice made while the last is still written starts from it; the file follows, one write at a time, holding the newest. */
    suspend fun keep(choices: ScanChoices) {
        kept.value = choices
        writing.withLock { withContext(Dispatchers.IO) { file.writeText(Json.encodeToString(ScanChoices.serializer(), kept.value)) } }
    }

    private fun read(): ScanChoices =
        file.takeIf { it.exists() }?.let { resultOf { Json.decodeFromString(ScanChoices.serializer(), it.readText()) }.getOrNull() } ?: ScanChoices()
}
