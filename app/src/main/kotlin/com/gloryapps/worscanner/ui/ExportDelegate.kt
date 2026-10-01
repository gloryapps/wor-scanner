package com.gloryapps.worscanner.ui

import android.content.Context
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.azhor.Link
import com.gloryapps.worscanner.azhor.Sending
import com.gloryapps.worscanner.capture.Exports
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.sharedFolders
import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The slice of a screen's ViewModel that exports, with a contract of its own; the ViewModel clears it with itself. */
class ExportDelegate(private val exports: Exports, private val context: Context, private val link: Link) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    private val _effects = Channel<ExportEffect>(Channel.BUFFERED)
    val effects: Flow<ExportEffect> = _effects.receiveAsFlow()

    init {
        scope.launch { link.linked.collect { linked -> _state.update { it.copy(linked = linked) } } }
    }

    fun begin(kept: Kept) = open(kept.outgoing(context))

    fun begin(readings: List<Kept>) = open(readings.outgoing(context))

    fun on(event: ExportEvent) {
        when (event) {
            is ExportEvent.Choose -> _state.update { it.copy(into = event.shared, failed = null) }
            ExportEvent.Save -> scope.launch { save() }
            ExportEvent.Share -> scope.launch { share() }
            ExportEvent.Send -> scope.launch { send() }
            ExportEvent.Close -> _state.update { it.copy(outgoing = null, failed = null) }
        }
    }

    fun clear() = scope.cancel()

    /* The folders are looked at again as the sheet opens: an emulator can mount its folder while the app runs. */
    private fun open(outgoing: Outgoing) {
        val shared = sharedFolders()
        _state.update { ExportUiState(outgoing, shared, it.into?.takeIf { into -> into in shared } ?: shared.firstOrNull(), linked = it.linked) }
    }

    private suspend fun save() {
        val state = _state.value
        val outgoing = state.outgoing ?: return
        resultOf { exports.toShared(state.into ?: error("no emulator shared folder on this device"), outgoing.files) }.fold(
            onSuccess = { landed ->
                _state.update { it.copy(outgoing = null, failed = null) }
                _effects.send(ExportEffect.Landed(landed.first().substringBeforeLast('/'), landed.size))
            },
            onFailure = { failure -> _state.update { it.copy(failed = failure.message ?: failure.toString()) } },
        )
    }

    private suspend fun send() {
        val outgoing = _state.value.outgoing ?: return
        if (_state.value.sending) return
        _state.update { it.copy(sending = true, failed = null) }
        val sent = link.send(outgoing.scans)
        _state.update { it.copy(sending = false) }

        if (sent == Sending.Sent) {
            _state.update { it.copy(outgoing = null) }
            _effects.send(ExportEffect.Sent(outgoing.scans.size))
        } else {
            _state.update { it.copy(failed = context.getString(unsent(sent))) }
        }
    }

    private suspend fun share() {
        val outgoing = _state.value.outgoing ?: return
        _effects.send(ExportEffect.Share(exports.shareIntent(outgoing.files)))
    }
}

/** Why a send did not reach the account, in words the player can act on. */
private fun unsent(sending: Sending): Int = when (sending) {
    Sending.Sent, Sending.Unanswered -> R.string.export_send_unanswered
    Sending.Unlinked -> R.string.export_send_unlinked
    Sending.Refused -> R.string.export_send_refused
    Sending.TooLarge -> R.string.export_send_too_large
}
