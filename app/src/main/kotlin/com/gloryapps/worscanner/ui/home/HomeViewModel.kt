package com.gloryapps.worscanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Readings
import com.gloryapps.worscanner.scan.Chosen
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import com.gloryapps.worscanner.ui.ExportDelegate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class HomeViewModel(
    session: CaptureSession,
    scanning: Scanning,
    private val chosen: Chosen,
    private val permissions: Permissions,
    private val readings: Readings,
    val export: ExportDelegate,
) : ViewModel() {
    private val kept = MutableStateFlow(readings.list())
    private val deleting = MutableStateFlow<List<Kept>>(emptyList())
    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        /* A scan that ends while the screen is in front is listed without waiting for the next return. */
        viewModelScope.launch { scanning.state.filterIsInstance<ScanState.Ended>().collect { kept.value = readings.list() } }
    }

    val state: StateFlow<HomeUiState> = combine(
        permissions.accessibilityOn,
        permissions.overlayAllowed,
        session.screen,
        scanning.state,
    ) { accessibility, overlay, screen, scan ->
        HomeUiState(
            accessibilityOn = accessibility,
            overlayAllowed = overlay,
            capturing = screen != null,
            running = scan as? ScanState.Running,
        )
    }
        .combine(chosen.kind) { state, kind -> state.copy(kind = kind) }
        .combine(kept) { state, readings -> state.copy(readings = readings) }
        .combine(deleting) { state, kept -> state.copy(deleting = kept) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun on(event: HomeEvent) {
        when (event) {
            HomeEvent.GrantAccessibility -> send(HomeEffect.OpenAccessibilitySettings)
            HomeEvent.GrantOverlay -> send(HomeEffect.OpenOverlaySettings)
            HomeEvent.Start -> send(HomeEffect.LaunchProjection)
            HomeEvent.Stop -> send(HomeEffect.StopCapture)
            is HomeEvent.Choose -> viewModelScope.launch { chosen.choose(event.kind) }
            is HomeEvent.Open -> send(HomeEffect.OpenReading(event.kept))
            is HomeEvent.Export -> export.begin(event.kept)
            HomeEvent.ExportAll -> export.begin(kept.value)
            is HomeEvent.Delete -> deleting.value = listOf(event.kept)
            HomeEvent.DeleteAll -> deleting.value = kept.value
            HomeEvent.ConfirmDelete -> {
                deleting.value.forEach(readings::delete)
                deleting.value = emptyList()
                kept.value = readings.list()
            }
            HomeEvent.CancelDelete -> deleting.value = emptyList()
        }
    }

    fun returned() {
        permissions.refresh()
        kept.value = readings.list()
    }

    private fun send(effect: HomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
