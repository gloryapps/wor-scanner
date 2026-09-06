package com.gloryapps.worscanner.walk

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** The hand: taps and drags over the game by gesture, since a Unity screen shows it no views. */
class TouchService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
