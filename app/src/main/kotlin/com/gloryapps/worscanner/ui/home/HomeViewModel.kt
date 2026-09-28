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
import com.gloryapps.worscanner.update.Update
import com.gloryapps.worscanner.update.Updates
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
    private val scanning: Scanning,
    private val chosen: Chosen,
    private val permissions: Permissions,
    private val readings: Readings,
    private val updates: Updates,
    val export: ExportDelegate,
) : ViewModel() {
    private val kept = MutableStateFlow<List<Kept>?>(null)
    private val deleting = MutableStateFlow<List<Kept>>(emptyList())
    /** Whether the deletion asked about is every one, which also clears what interrupted scans left. */
    private var all = false
    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        /* A scan that ends while the screen is in front is listed without waiting for the next return. */
        viewModelScope.launch { scanning.state.filterIsInstance<ScanState.Ended>().collect { relist() } }
        viewModelScope.launch { updates.check() }
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
        .combine(updates.state) { state, update -> state.copy(update = update) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            /* What is known at once, so a start does not flash nothing granted. */
            HomeUiState(
                accessibilityOn = permissions.accessibilityOn.value,
                overlayAllowed = permissions.overlayAllowed.value,
                capturing = session.screen.value != null,
                kind = chosen.kind.value,
                running = scanning.state.value as? ScanState.Running,
                update = updates.state.value,
            ),
        )

    fun on(event: HomeEvent) {
        when (event) {
            HomeEvent.GrantAccessibility -> send(HomeEffect.OpenAccessibilitySettings)
            HomeEvent.GrantOverlay -> send(HomeEffect.OpenOverlaySettings)
            HomeEvent.Start -> send(HomeEffect.LaunchProjection)
            HomeEvent.Stop -> send(HomeEffect.StopCapture)
            is HomeEvent.Choose -> chosen.choose(event.kind)
            is HomeEvent.Open -> send(HomeEffect.OpenReading(event.kept))
            is HomeEvent.Export -> export.begin(event.kept)
            HomeEvent.ExportAll -> export.begin(kept.value.orEmpty())
            is HomeEvent.Delete -> ask(listOf(event.kept), all = false)
            HomeEvent.DeleteAll -> ask(kept.value.orEmpty(), all = true)
            HomeEvent.ConfirmDelete -> {
                val going = deleting.value
                val sweep = all && scanning.state.value !is ScanState.Running
                ask(emptyList(), all = false)
                viewModelScope.launch {
                    going.forEach { readings.delete(it) }
                    if (sweep) readings.sweep()
                    relist()
                }
            }
            HomeEvent.CancelDelete -> ask(emptyList(), all = false)
            HomeEvent.Update -> when (val update = updates.state.value) {
                is Update.Available -> viewModelScope.launch { updates.download()?.let { _effects.send(HomeEffect.Install(it)) } }
                is Update.Failed -> send(HomeEffect.OpenPage(update.release.page))
                Update.None, is Update.Downloading -> Unit
            }
        }
    }

    override fun onCleared() = export.clear()

    fun returned() {
        permissions.refresh()
        viewModelScope.launch { relist() }
    }

    private fun ask(readings: List<Kept>, all: Boolean) {
        this.all = all
        deleting.value = readings
    }

    private suspend fun relist() {
        kept.value = readings.list()
    }

    private fun send(effect: HomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
