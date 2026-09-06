package com.gloryapps.worscanner.ui.prepare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.walk.TouchState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PrepareState(
    val accessibilityOn: Boolean = false,
    val overlayAllowed: Boolean = false,
    val capturing: Boolean = false,
) {
    val ready: Boolean get() = accessibilityOn && overlayAllowed && capturing
}

class PrepareViewModel(
    touch: TouchState,
    session: CaptureSession,
    private val permissions: Permissions,
) : ViewModel() {
    val state: StateFlow<PrepareState> = combine(
        touch.connected,
        permissions.overlayAllowed,
        session.screen,
    ) { accessibilityOn, overlayAllowed, screen ->
        PrepareState(accessibilityOn, overlayAllowed, capturing = screen != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrepareState())

    fun returned() = permissions.refresh()
}
