package com.gloryapps.worscanner.ui

import android.content.Context
import android.content.Intent
import com.gloryapps.worscanner.capture.Exports
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Outbound
import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The slice of a screen's ViewModel that exports: what is on its way out, the two doors, and what the last one said. */
class ExportDelegate(private val exports: Exports, private val context: Context) {
    private val _outgoing = MutableStateFlow<Outgoing?>(null)
    val outgoing: StateFlow<Outgoing?> = _outgoing.asStateFlow()

    private val _saved = MutableStateFlow<Saved?>(null)
    val saved: StateFlow<Saved?> = _saved.asStateFlow()

    fun begin(kept: Kept) {
        _outgoing.value = kept.outgoing(context)
    }

    fun begin(readings: List<Kept>) {
        _outgoing.value = readings.outgoing(context)
    }

    fun toShared(files: List<Outbound>) = save { exports.toShared(files) }

    fun shareIntent(files: List<Outbound>): Intent = exports.shareIntent(files)

    fun forget() {
        _outgoing.value = null
        _saved.value = null
    }

    private fun save(export: () -> List<String>) {
        _saved.value = resultOf(export).fold(
            onSuccess = { Saved.Into(it.first().substringBeforeLast('/'), it.size) },
            onFailure = { Saved.Failed(it.message ?: it.toString()) },
        )
    }
}
