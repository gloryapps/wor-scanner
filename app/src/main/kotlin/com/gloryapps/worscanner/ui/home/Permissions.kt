package com.gloryapps.worscanner.ui.home

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The grants the system holds, re-read whenever the screen comes back from the settings that give them. */
class Permissions(private val context: Context) {
    private val _overlayAllowed = MutableStateFlow(overlay())
    val overlayAllowed: StateFlow<Boolean> = _overlayAllowed.asStateFlow()

    private val _accessibilityOn = MutableStateFlow(accessibility())
    val accessibilityOn: StateFlow<Boolean> = _accessibilityOn.asStateFlow()

    fun refresh() {
        _overlayAllowed.value = overlay()
        _accessibilityOn.value = accessibility()
    }

    private fun overlay() = Settings.canDrawOverlays(context)

    /* The system's word, not the service's: the service may not have been rebound yet after the process died. */
    private fun accessibility() = context.getSystemService(AccessibilityManager::class.java)
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}
