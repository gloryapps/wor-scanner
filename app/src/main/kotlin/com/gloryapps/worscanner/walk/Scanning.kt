package com.gloryapps.worscanner.walk

import android.content.Context
import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.scanner.Screen
import com.gloryapps.worscanner.scanner.TextReader
import com.gloryapps.worscanner.scanner.walk.Outcome
import com.gloryapps.worscanner.scanner.walk.Progress
import com.gloryapps.worscanner.scanner.walk.ScanEntry
import com.gloryapps.worscanner.scanner.walk.StorageLayout
import com.gloryapps.worscanner.scanner.walk.Walk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** What a scan under way says of itself, for the notification and the screens to render. */
sealed interface ScanState {
    data object Idle : ScanState

    data class Running(val progress: Progress) : ScanState

    data class Ended(val outcome: Outcome, val file: File) : ScanState
}

/** Runs one walk with the hand and eyes of the moment and writes what it found. */
class Scanning(
    private val context: Context,
    private val session: CaptureSession,
    private val touch: TouchState,
    private val reader: TextReader,
) {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    suspend fun run() {
        val screen: Screen = checkNotNull(session.screen.value) { "no capture session" }
        val hand = checkNotNull(touch.hand.value) { "accessibility service not bound" }
        val layout = StorageLayout()
        val writer = ScanWriter(context, layout)
        val first = screen.capture()

        val entries = mutableListOf<ScanEntry>()
        var outcome: Outcome? = null

        _state.value = ScanState.Running(Progress(0, 0))
        try {
            outcome = Walk(screen, hand, reader, writer.keeper, layout).run(entries) { _state.value = ScanState.Running(it) }
        } finally {
            /* A cancelled walk ends by its exception; what it read before is still worth writing. */
            val ended = outcome ?: Outcome.Stopped(Outcome.Reason.CANCELLED, entries)
            _state.value = ScanState.Ended(ended, writer.write(first.width, first.height, ended))
        }
    }
}
