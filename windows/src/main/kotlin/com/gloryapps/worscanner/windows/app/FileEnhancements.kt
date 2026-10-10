package com.gloryapps.worscanner.windows.app

import com.gloryapps.worscanner.scanner.account.Enhancement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** The bands of enhancement the gear scan keeps, in a file of their own in the app's folder; +16 alone, the gear that counts, until the player chooses. */
internal class FileEnhancements(private val file: File) {
    private val kept = MutableStateFlow(read())
    val chosen: StateFlow<Set<Enhancement>> = kept

    /** One band in or out; the last one chosen stays, since a gear scan of none sends the lab nothing to keep. */
    suspend fun toggle(band: Enhancement) = withContext(Dispatchers.IO) {
        val next = if (band in kept.value) kept.value - band else kept.value + band
        if (next.isNotEmpty()) {
            file.writeText(next.joinToString("\n") { it.name })
            kept.value = next
        }
    }

    private fun read(): Set<Enhancement> =
        file.takeIf { it.exists() }?.readLines()?.mapNotNull { line -> Enhancement.entries.firstOrNull { it.name == line.trim() } }?.toSet()?.ifEmpty { null }
            ?: setOf(Enhancement.AT_16)
}
