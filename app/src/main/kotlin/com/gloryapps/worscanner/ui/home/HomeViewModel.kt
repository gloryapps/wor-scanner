package com.gloryapps.worscanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Readings
import com.gloryapps.worscanner.scan.Chosen
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.Exporting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val accessibilityOn: Boolean = false,
    val overlayAllowed: Boolean = false,
    val capturing: Boolean = false,
    val kind: Kind = Chosen.FIRST,
    /** The scan under way, for the header; null when none is running. */
    val running: ScanState.Running? = null,
    val readings: List<Kept> = emptyList(),
) {
    /** A scan can only start once the system lets the app both see the screen and touch it. */
    val ready: Boolean get() = accessibilityOn && overlayAllowed
}

class HomeViewModel(
    session: CaptureSession,
    scanning: Scanning,
    private val chosen: Chosen,
    private val permissions: Permissions,
    private val readings: Readings,
    val exporting: Exporting,
) : ViewModel() {
    private val kept = MutableStateFlow(readings.list())

    val state: StateFlow<HomeState> = combine(
        permissions.accessibilityOn,
        permissions.overlayAllowed,
        session.screen,
        scanning.state,
    ) { accessibility, overlay, screen, scan ->
        HomeState(
            accessibilityOn = accessibility,
            overlayAllowed = overlay,
            capturing = screen != null,
            running = scan as? ScanState.Running,
        )
    }
        .combine(chosen.kind) { state, kind -> state.copy(kind = kind) }
        .combine(kept) { state, readings -> state.copy(readings = readings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun returned() {
        permissions.refresh()
        kept.value = readings.list()
    }

    fun choose(kind: Kind) {
        viewModelScope.launch { chosen.choose(kind) }
    }

    fun delete(kept: Kept) {
        readings.delete(kept)
        this.kept.value = readings.list()
    }
}

