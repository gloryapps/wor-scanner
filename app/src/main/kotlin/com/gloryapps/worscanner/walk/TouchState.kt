package com.gloryapps.worscanner.walk

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether the system has the accessibility service bound, which only the service itself knows. */
class TouchState {
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    fun bound() {
        _connected.value = true
    }

    fun unbound() {
        _connected.value = false
    }
}
