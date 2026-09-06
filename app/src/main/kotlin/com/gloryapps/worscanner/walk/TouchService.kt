package com.gloryapps.worscanner.walk

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import org.koin.android.ext.android.inject

/** The hand: taps and drags over the game by gesture, since a Unity screen shows it no views. */
class TouchService : AccessibilityService() {
    private val state: TouchState by inject()

    override fun onServiceConnected() {
        state.bound()
    }

    override fun onDestroy() {
        state.unbound()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
