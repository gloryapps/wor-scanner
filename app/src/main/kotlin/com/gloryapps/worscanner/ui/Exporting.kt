package com.gloryapps.worscanner.ui

import android.content.Intent
import com.gloryapps.worscanner.capture.Exports
import com.gloryapps.worscanner.capture.Outbound
import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The two doors out of the app and what the last one said, for every screen that can export. */
class Exporting(private val exports: Exports) {
    private val _saved = MutableStateFlow<Saved?>(null)
    val saved: StateFlow<Saved?> = _saved.asStateFlow()

    fun toShared(files: List<Outbound>) = save { exports.toShared(files) }

    fun shareIntent(files: List<Outbound>): Intent = exports.shareIntent(files)

    fun forget() {
        _saved.value = null
    }

    private fun save(export: () -> List<String>) {
        _saved.value = resultOf(export).fold(
            onSuccess = { Saved.Into(it.first().substringBeforeLast('/'), it.size) },
            onFailure = { Saved.Failed(it.message ?: it.toString()) },
        )
    }
}
