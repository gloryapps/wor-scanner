package com.gloryapps.worscanner.windows.azhor

import com.gloryapps.worscanner.scanner.azhor.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** The token in a file of its own in the app's folder: read as the app starts, written on every change. */
internal class FileTokenStore(private val file: File) : TokenStore {
    private val kept = MutableStateFlow(file.takeIf { it.exists() }?.readText()?.trim()?.ifEmpty { null })
    override val token: StateFlow<String?> = kept

    override suspend fun keep(token: String?) = withContext(Dispatchers.IO) {
        if (token == null) file.delete() else file.writeText(token)
        kept.value = token
    }
}
