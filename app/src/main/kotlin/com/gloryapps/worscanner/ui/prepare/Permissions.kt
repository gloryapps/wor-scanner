package com.gloryapps.worscanner.ui.prepare

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The overlay grant, re-read whenever the screen comes back from the system settings. */
class Permissions(private val context: Context) {
    private val _overlayAllowed = MutableStateFlow(Settings.canDrawOverlays(context))
    val overlayAllowed: StateFlow<Boolean> = _overlayAllowed.asStateFlow()

    fun refresh() {
        _overlayAllowed.value = Settings.canDrawOverlays(context)
    }
}
