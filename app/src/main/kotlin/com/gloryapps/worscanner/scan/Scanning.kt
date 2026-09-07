package com.gloryapps.worscanner.scan

import android.content.Context
import com.gloryapps.worscanner.capture.BitmapFrame
import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.kinds.Scannable
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.Progress
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import com.gloryapps.worscanner.scanner.scan.scannable
import com.gloryapps.worscanner.scanner.senses.Screen
import com.gloryapps.worscanner.scanner.senses.TextReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** What a scan under way says of itself, for the notification and the screens to render. */
sealed interface ScanState {
    data object Idle : ScanState

    data class Running(val progress: Progress) : ScanState

    data class Ended(val outcome: Outcome<*>, val file: File) : ScanState
}

/** Runs one scan of a kind with the hand and eyes of the moment and writes what it found. */
class Scanning(
    private val context: Context,
    private val session: CaptureSession,
    private val touch: TouchState,
    private val reader: TextReader,
) {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    suspend fun run(kind: Kind) = run(kind, kind.scannable())

    /* The record's type is fixed here, for the length of one scan, and the app above never sees it. */
    private suspend fun <T> run(kind: Kind, scannable: Scannable<T>) {
        val screen: Screen = checkNotNull(session.screen.value) { "no capture session" }
        val hand = checkNotNull(touch.hand.value) { "accessibility service not bound" }
        val writer = ScanWriter(context, kind, scannable)
        val first = screen.capture() as BitmapFrame

        val entries = mutableListOf<ScanEntry<T>>()
        var outcome: Outcome<T>? = null

        _state.value = ScanState.Running(Progress(0, 0))
        try {
            outcome = Scan(scannable, screen, hand, reader, writer.keeper).run(entries) { _state.value = ScanState.Running(it) }
        } finally {
            /* A cancelled scan ends by its exception; what it read before is still worth writing. */
            val ended = outcome ?: Outcome.Stopped(Outcome.Reason.CANCELLED, entries, "stopped by the user")
            _state.value = ScanState.Ended(ended, writer.write(first, ended))
        }
    }
}
