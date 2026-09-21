package com.gloryapps.worscanner.ui

import android.content.Context
import android.content.Intent
import com.gloryapps.worscanner.capture.Exports
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Outbound
import com.gloryapps.worscanner.capture.SharedFolder
import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The slice of a screen's ViewModel that exports: what is on its way out, the two doors, and what the last one said. */
class ExportDelegate(private val exports: Exports, private val context: Context) {
    private val _outgoing = MutableStateFlow<Outgoing?>(null)
    val outgoing: StateFlow<Outgoing?> = _outgoing.asStateFlow()

    /** The shared folders this device has, and the one the next save lands in: the first found until the user says otherwise. */
    val shared: List<SharedFolder> = exports.shared()

    private val _into = MutableStateFlow(shared.firstOrNull())
    val into: StateFlow<SharedFolder?> = _into.asStateFlow()

    private val _saved = MutableStateFlow<Saved?>(null)
    val saved: StateFlow<Saved?> = _saved.asStateFlow()

    fun begin(kept: Kept) {
        _outgoing.value = kept.outgoing(context)
    }

    fun begin(readings: List<Kept>) {
        _outgoing.value = readings.outgoing(context)
    }

    fun choose(shared: SharedFolder) {
        _into.value = shared
    }

    fun toShared(files: List<Outbound>) = save {
        exports.toShared(_into.value ?: error("no emulator shared folder on this device"), files)
    }

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
