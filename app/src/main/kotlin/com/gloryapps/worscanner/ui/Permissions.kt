package com.gloryapps.worscanner.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.SettingsAccessibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.gloryapps.worscanner.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A grant the scanner asks the system for: how the app names it, why it asks, and whether a scan can start without it. */
enum class Permission(val icon: ImageVector, @StringRes val label: Int, @StringRes val why: Int, val needed: Boolean) {
    ACCESSIBILITY(Icons.Outlined.SettingsAccessibility, R.string.permission_accessibility, R.string.permission_accessibility_why, needed = true) {
        /* The setting the player turned on, not the services bound: the system rebinds this one only after the process starts. */
        override fun given(context: Context) = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty()
            .split(':')
            .any { ComponentName.unflattenFromString(it)?.packageName == context.packageName }
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

/** Whether these grants let a scan start: the system lets the app both draw over the game and touch it. */
val Set<Permission>.canScan: Boolean
    get() = Permission.entries.all { !it.needed || it in this }

/**
 * The system's own way to give each grant, its settings screen or its dialog. `onAnswered` runs as the
 * dialog closes; a settings screen is answered by the screen's return.
 */
@Composable
fun rememberGrant(onAnswered: () -> Unit): (Permission) -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val answered by rememberUpdatedState(onAnswered)
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { given ->
        /* Refused twice, Android stops showing its dialog, and the app's notification settings are the way left. */
        if (!given && activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == false) {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }
        answered()
    }

    return { permission ->
        when (permission) {
            Permission.ACCESSIBILITY -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Permission.OVERLAY -> context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
            Permission.NOTIFICATIONS -> notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
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
