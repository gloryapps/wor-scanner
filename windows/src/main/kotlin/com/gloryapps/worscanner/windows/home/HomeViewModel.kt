package com.gloryapps.worscanner.windows.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.scanner.azhor.Send
import com.gloryapps.worscanner.scanner.azhor.Sender
import com.gloryapps.worscanner.scanner.runs.Kept
import com.gloryapps.worscanner.scanner.runs.ReadScreen
import com.gloryapps.worscanner.scanner.runs.Readings
import com.gloryapps.worscanner.scanner.runs.ScanState
import com.gloryapps.worscanner.scanner.runs.Scanning
import com.gloryapps.worscanner.scanner.runs.scans
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.ui.said
import com.gloryapps.worscanner.windows.game.Desktop
import com.gloryapps.worscanner.windows.game.GameWatch
import com.gloryapps.worscanner.windows.hand.EscapeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class HomeViewModel(
    private val watch: GameWatch,
    private val readScreen: ReadScreen,
    private val scanning: Scanning,
    private val readings: Readings,
    private val sender: Sender,
) : ViewModel() {
    private val game = if (Desktop.present) watch.game.map { window -> window?.let { Game.Open(it.client.width, it.client.height) } ?: Game.Closed } else flowOf(Game.NotWindows)
    private val local = MutableStateFlow(HomeState())
    private var scan: Job? = null

    val state: StateFlow<HomeState> = combine(game, local) { game, local -> local.copy(game = game) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), HomeState())

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            shown(readings.list().firstOrNull { it is Kept.Scan } as? Kept.Scan)
            scanning.state.collect { state ->
                when (state) {
                    ScanState.Idle -> Unit
                    is ScanState.Running -> local.update { it.copy(progress = state.said()) }
                    is ScanState.Ended -> {
                        val kept = readings.kept(state.stamp)
                        local.update { it.copy(said = state.said(), kept = kept?.files?.firstOrNull()?.parentFile) }
                        /* A scan that read nothing, or failed, has nothing to send. */
                        if (state.outcome !is Outcome.Failed && state.outcome.entries.isNotEmpty()) shown(kept as? Kept.Scan)
                    }
                }
            }
        }
    }

    fun on(event: HomeEvent) {
        when (event) {
            is HomeEvent.Choose -> local.update { it.copy(kind = event.kind) }
            HomeEvent.Read -> read()
            HomeEvent.Scan -> scan()
            HomeEvent.OpenKept -> local.value.kept?.let { folder -> viewModelScope.launch { _effects.send(HomeEffect.Open(folder)) } }
            HomeEvent.Send -> local.value.scan?.let { scan -> viewModelScope.launch { sender.send(scan.scans(), ::sending) } }
            is HomeEvent.LinkAndSend -> local.value.scan?.let { scan -> viewModelScope.launch { sender.linkAndSend(event.code, scan.scans(), ::sending) } }
            HomeEvent.Next -> local.update { it.copy(scan = null) }
        }
    }

    /** The scan the send card is for, on the link step while the scanner is not linked; its folder the one opened. */
    private suspend fun shown(scan: Kept.Scan?) {
        scan ?: return
        val send = sender.start()
        local.update { it.copy(scan = scan, send = send, kept = scan.files.firstOrNull()?.parentFile ?: it.kept) }
    }

    private fun sending(send: Send) = local.update { it.copy(send = send) }

    /* The game is brought in front first: the app's own window may stand over it, and the desktop is what is copied. */
    private fun read() {
        if (local.value.reading || local.value.scanning) return
        local.update { it.copy(reading = true) }
        viewModelScope.launch {
            watch.game.value?.let { Desktop.bringToFront(it.handle) }
            delay(FRONT_MS)
            /* The copy waits for the game in front; a game Windows would not bring there is a Read that failed. */
            val read = withContext(Dispatchers.Default) { withTimeoutOrNull(READ_WAIT_MS) { readScreen.now(local.value.kind) } }
                ?: Result.failure(IllegalStateException("the game did not come to the front"))
            local.update { it.copy(reading = false, said = read.said(), kept = read.getOrNull()?.kept?.files?.firstOrNull()?.parentFile) }
            _effects.send(HomeEffect.Front)
        }
    }

    /*
     * The sign goes up over the game before the game is brought in front, so that it never takes the
     * front from it. Only the sign's Stop, this screen's, or Esc from the game stops the scan.
     */
    private fun scan() {
        if (scan?.isActive == true) {
            scan?.cancel()
            return
        }
        val game = watch.game.value ?: return
        local.update { it.copy(over = game.client, progress = null) }
        scan = viewModelScope.launch {
            val front = launch { followFront(game.handle) }
            try {
                delay(FRONT_MS)
                Desktop.bringToFront(game.handle)
                delay(FRONT_MS)
                withContext(Dispatchers.Default) { scanning.run(local.value.kind) }
            } finally {
                front.cancel()
                local.update { it.copy(over = null, paused = false) }
                _effects.trySend(HomeEffect.Front)
            }
        }
    }

    /*
     * While the game is in front Esc is the scan's, so the player stops it from there; while another
     * window is, the scan waits and Esc is that window's again, so no Esc meant for it stops a scan.
     */
    private suspend fun followFront(game: Long) {
        var escape: EscapeKey? = null
        try {
            while (true) {
                val front = Desktop.inFront(game)
                if (front && escape == null) escape = EscapeKey { scan?.cancel() }
                if (!front) escape = escape?.let { it.close(); null }
                local.update { it.copy(paused = !front) }
                delay(FOLLOW_MS)
            }
        } finally {
            escape?.close()
        }
    }

    private companion object {
        const val STOP_AFTER_MS = 5_000L
        /** Long enough for the desktop to draw what was brought in front where the app's window stood. */
        const val FRONT_MS = 400L
        const val FOLLOW_MS = 300L
        const val READ_WAIT_MS = 15_000L
    }
}
