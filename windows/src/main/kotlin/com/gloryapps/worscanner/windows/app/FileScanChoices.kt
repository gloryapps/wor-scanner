package com.gloryapps.worscanner.windows.app

import com.gloryapps.worscanner.scanner.account.ScanChoices
import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** What the player chose a scan sends, in a file of its own in the app's folder; the scan's defaults until he chooses, or where the file will not read. */
internal class FileScanChoices(private val file: File) {
    private val kept = MutableStateFlow(read())
    val chosen: StateFlow<ScanChoices> = kept

    suspend fun keep(choices: ScanChoices) = withContext(Dispatchers.IO) {
        file.writeText(Json.encodeToString(ScanChoices.serializer(), choices))
        kept.value = choices
    }

    private fun read(): ScanChoices =
        file.takeIf { it.exists() }?.let { resultOf { Json.decodeFromString(ScanChoices.serializer(), it.readText()) }.getOrNull() } ?: ScanChoices()
}
