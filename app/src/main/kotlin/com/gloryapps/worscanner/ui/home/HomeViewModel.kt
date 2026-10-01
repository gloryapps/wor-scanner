package com.gloryapps.worscanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import com.gloryapps.worscanner.update.Update
import com.gloryapps.worscanner.update.Updates
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class HomeViewModel(
    session: CaptureSession,
    scanning: Scanning,
    private val permissions: Permissions,
    private val updates: Updates,
) : ViewModel() {
    private val asking = MutableStateFlow(false)
    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch { updates.check() }
    }

    val state: StateFlow<HomeUiState> = combine(
        permissions.granted,
        asking,
        session.screen,
        scanning.state,
        updates.state,
    ) { granted, asking, screen, scan, update ->
        HomeUiState(
            granted = granted,
            asking = asking,
            capturing = screen != null,
            running = scan as? ScanState.Running,
            update = update,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            /* What is known at once, so a start does not flash nothing granted. */
            HomeUiState(
                granted = permissions.granted.value,
                capturing = session.screen.value != null,
                running = scanning.state.value as? ScanState.Running,
                update = updates.state.value,
            ),
        )

    fun on(event: HomeEvent) {
        when (event) {
            is HomeEvent.Grant -> send(HomeEffect.Grant(event.permission))
            HomeEvent.Start -> if (state.value.missing.isEmpty()) send(HomeEffect.LaunchProjection) else asking.value = true
            HomeEvent.Begin -> {
                asking.value = false
                send(HomeEffect.LaunchProjection)
            }
            HomeEvent.Dismiss -> asking.value = false
            HomeEvent.Stop -> send(HomeEffect.StopCapture)
            HomeEvent.Earlier -> send(HomeEffect.OpenEarlier)
            HomeEvent.Update -> when (val update = updates.state.value) {
                is Update.Available -> viewModelScope.launch { updates.download()?.let { _effects.send(HomeEffect.Install(it)) } }
                is Update.Failed -> send(HomeEffect.OpenPage(update.release.page))
                Update.None, is Update.Downloading -> Unit
            }
        }
    }

    fun returned() = permissions.refresh()

    private fun send(effect: HomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
