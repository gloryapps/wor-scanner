package com.gloryapps.worscanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.azhor.Link
import com.gloryapps.worscanner.azhor.Linking
import com.gloryapps.worscanner.azhor.Sending
import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Readings
import com.gloryapps.worscanner.capture.sharedFolders
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.ui.ExportDelegate
import com.gloryapps.worscanner.ui.Permissions
import com.gloryapps.worscanner.ui.scans
import com.gloryapps.worscanner.update.Update
import com.gloryapps.worscanner.update.Updates
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HomeViewModel(
    stage: Stage?,
    session: CaptureSession,
    scanning: Scanning,
    private val permissions: Permissions,
    private val updates: Updates,
    private val readings: Readings,
    private val link: Link,
    val export: ExportDelegate,
) : ViewModel() {
    private val asking = MutableStateFlow(false)
    private val just = MutableStateFlow<JustScanned?>(null)
    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch { updates.check() }
        /* A scan under way is no longer the one that just ended; the next to end takes its place. */
        viewModelScope.launch {
            scanning.state.collect { scan ->
                when (scan) {
                    is ScanState.Running -> just.value = null
                    is ScanState.Ended -> just.value = justScanned(scan)
                    ScanState.Idle -> Unit
                }
            }
        }
        when (stage) {
            Stage.JustScanned -> viewModelScope.launch { just.value = newest() }
            Stage.Asking -> asking.value = true
            null -> Unit
        }
    }

    val state: StateFlow<HomeUiState> = combine(
        combine(permissions.grants, asking, ::Pair),
        session.screen,
        scanning.state,
        updates.state,
        just,
    ) { (grants, asking), screen, scan, update, just ->
        HomeUiState(
            grants = grants,
            asking = asking,
            capturing = screen != null,
            running = scan as? ScanState.Running,
            update = update,
            justScanned = just,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            /* What is known at once, so a start does not flash nothing granted. */
            HomeUiState(
                grants = permissions.grants.value,
                capturing = session.screen.value != null,
                running = scanning.state.value as? ScanState.Running,
                update = updates.state.value,
            ),
        )

    fun on(event: HomeEvent) {
        when (event) {
            is HomeEvent.Grant -> send(HomeEffect.Grant(event.permission))
            HomeEvent.Start -> if (state.value.grants.missing.isEmpty()) send(HomeEffect.LaunchProjection) else asking.value = true
            HomeEvent.Begin -> {
                asking.value = false
                send(HomeEffect.LaunchProjection)
            }
            HomeEvent.Dismiss -> asking.value = false
            HomeEvent.Stop -> send(HomeEffect.StopCapture)
            HomeEvent.Send -> viewModelScope.launch { if (link.linked.first()) deliver() else sending(Send.Code()) }
            is HomeEvent.LinkAndSend -> viewModelScope.launch {
                sending(Send.Underway)
                when (val linking = link.link(event.code)) {
                    is Linking.Linked -> deliver()
                    Linking.Refused, Linking.Unanswered -> sending(Send.Code(failed = linking))
                }
            }
            HomeEvent.Save -> just.value?.let { scanned -> scanned.shared?.let { export.save(scanned.scan, it) } }
            HomeEvent.Share -> just.value?.let { export.share(it.scan) }
            HomeEvent.Next -> just.value = null
            HomeEvent.Earlier -> send(HomeEffect.OpenEarlier)
            HomeEvent.HowTo -> send(HomeEffect.OpenHowTo)
            HomeEvent.Debug -> send(HomeEffect.OpenDebug)
            HomeEvent.Update -> when (val update = updates.state.value) {
                is Update.Available -> viewModelScope.launch { updates.download()?.let { _effects.send(HomeEffect.Install(it)) } }
                is Update.Failed -> send(HomeEffect.OpenPage(update.release.page))
                Update.None, is Update.Downloading -> Unit
            }
        }
    }

    override fun onCleared() = export.clear()

    fun returned() = permissions.refresh()

    /** The screen was left: a scan already sent is put away, and the next return teaches again. */
    fun left() {
        just.update { it?.takeUnless { scanned -> scanned.send == Send.Sent } }
    }

    /* A scan that read nothing, or failed, has nothing to send: the list keeps it, and Home teaches. */
    private suspend fun justScanned(ended: ScanState.Ended): JustScanned? {
        val outcome = ended.outcome
        if (outcome is Outcome.Failed || outcome.entries.isEmpty()) return null

        return shown(readings.kept(ended.stamp), ended.kind, touched = (outcome as? Outcome.Stopped)?.reason == Outcome.Reason.CANCELLED)
    }

    /** The newest scan kept, as if it had just ended; null while none is kept. */
    private suspend fun newest(): JustScanned? = shown(readings.list().firstOrNull { it is Kept.Scan }, kind = null, touched = false)

    /**
     * A kept scan as Home shows one that just ended, of `kind` or the kind its file names, on the link
     * step while the scanner is not linked; null for anything else.
     */
    private suspend fun shown(kept: Kept?, kind: Kind?, touched: Boolean): JustScanned? {
        val scan = kept as? Kept.Scan ?: return null
        val send = if (link.linked.first()) Send.Idle else Send.Code()

        return JustScanned(scan, kind ?: scan.kind ?: return null, touched, sharedFolders().firstOrNull(), send)
    }

    private suspend fun deliver() {
        val scan = just.value?.scan ?: return
        sending(Send.Underway)
        val sent = link.send(scan.scans())
        sending(if (sent == Sending.Sent) Send.Sent else Send.Unsent(sent))
    }

    private fun sending(send: Send) {
        just.update { it?.copy(send = send) }
    }

    private fun send(effect: HomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
