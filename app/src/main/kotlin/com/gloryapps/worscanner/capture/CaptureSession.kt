package com.gloryapps.worscanner.capture

import com.gloryapps.worscanner.scanner.senses.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The projection the user granted, while the capture service holds it; null between sessions. */
class CaptureSession {
    private val _screen = MutableStateFlow<Screen?>(null)
    val screen: StateFlow<Screen?> = _screen.asStateFlow()

    fun opened(screen: Screen) {
        _screen.value = screen
    }

    fun closed() {
        _screen.value = null
    }
}
