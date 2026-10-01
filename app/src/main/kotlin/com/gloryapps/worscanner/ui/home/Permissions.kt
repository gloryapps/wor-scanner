package com.gloryapps.worscanner.ui.home

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.SettingsAccessibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.gloryapps.worscanner.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A grant the scanner asks the system for: how the app names it, why it asks, and whether a scan can start without it. */
enum class Permission(val icon: ImageVector, @StringRes val label: Int, @StringRes val why: Int, val needed: Boolean) {
    ACCESSIBILITY(Icons.Outlined.SettingsAccessibility, R.string.permission_accessibility, R.string.permission_accessibility_why, needed = true) {
        /* The system's word, not the service's: the service may not have been rebound yet after the process died. */
        override fun given(context: Context) = context.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
    },
    OVERLAY(Icons.Outlined.PictureInPicture, R.string.permission_overlay, R.string.permission_overlay_why, needed = true) {
        override fun given(context: Context) = Settings.canDrawOverlays(context)
    },

    /* A scan runs without it, but from Android 13 the notification that holds its Stop is hidden. */
    NOTIFICATIONS(Icons.Outlined.Notifications, R.string.permission_notifications, R.string.permission_notifications_why, needed = false) {
        override fun given(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    };

    abstract fun given(context: Context): Boolean
}

/** The grants the system holds, re-read whenever the screen comes back from the settings that give them. */
class Permissions(private val context: Context) {
    private val _granted = MutableStateFlow(given())
    val granted: StateFlow<Set<Permission>> = _granted.asStateFlow()

    fun refresh() {
        _granted.value = given()
    }

    private fun given(): Set<Permission> = Permission.entries.filterTo(mutableSetOf()) { it.given(context) }
}
