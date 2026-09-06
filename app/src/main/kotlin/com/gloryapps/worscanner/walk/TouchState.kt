package com.gloryapps.worscanner.walk

import com.gloryapps.worscanner.scanner.Touch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The hand while the system has the accessibility service bound, which only the service itself knows. */
class TouchState {
    private val _hand = MutableStateFlow<Touch?>(null)
    val hand: StateFlow<Touch?> = _hand.asStateFlow()

    fun bound(hand: Touch) {
        _hand.value = hand
    }

    fun unbound() {
        _hand.value = null
    }
}
