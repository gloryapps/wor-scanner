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
import com.gloryapps.worscanner.scan.TouchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A grant the scanner asks the system for: how the app names it, why it asks, and whether a scan can start without it. */
enum class Permission(val icon: ImageVector, @StringRes val label: Int, @StringRes val why: Int, val needed: Boolean) {
    ACCESSIBILITY(Icons.Outlined.SettingsAccessibility, R.string.permission_accessibility, R.string.permission_accessibility_why, needed = true) {
        override fun given(context: Context) = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty()
            .split(':')
            .any { ComponentName.unflattenFromString(it)?.packageName == context.packageName }

        /* On in the settings is not enough: after an update or a force-stop, Android can leave the service on there and not run it. */
        override fun standing(context: Context, touch: TouchState) = when {
            !given(context) -> Standing.OFF
            touch.hand.value == null -> Standing.STALLED
            else -> Standing.ON
        }
    },
    OVERLAY(Icons.Outlined.PictureInPicture, R.string.permission_overlay, R.string.permission_overlay_why, needed = true) {
        override fun given(context: Context) = Settings.canDrawOverlays(context)
    },

    /* A scan runs without it, but from Android 13 the notification that holds its Stop is hidden. */
    NOTIFICATIONS(Icons.Outlined.Notifications, R.string.permission_notifications, R.string.permission_notifications_why, needed = false) {
        override fun given(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    };

    /** Whether Android's settings give it. */
    protected abstract fun given(context: Context): Boolean

    /** Where it stands; for most, whether the settings give it. */
    open fun standing(context: Context, touch: TouchState): Standing = if (given(context)) Standing.ON else Standing.OFF
}

/** Where a grant stands with the system. */
enum class Standing {
    ON,

    /** On in Android's settings, but not running: turning it off and on there starts it. */
    STALLED,
    OFF,
}

/** What the way to a grant says while it is not on; null once it is. */
val Standing.action: Int?
    @StringRes get() = when (this) {
        Standing.ON -> null
        Standing.STALLED -> R.string.permission_restart
        Standing.OFF -> R.string.permission_turn_on
    }

/** Where every grant stands; one the map does not name is off. */
data class Grants(private val standing: Map<Permission, Standing> = emptyMap()) {
    operator fun get(permission: Permission): Standing = standing[permission] ?: Standing.OFF

    /** Whether a scan can start: the system lets the app both draw over the game and touch it. */
    val canScan: Boolean get() = Permission.entries.all { !it.needed || this[it] == Standing.ON }

    /** The grants not on, in the order they are asked for. */
    val missing: List<Permission> get() = Permission.entries.filter { this[it] != Standing.ON }

    companion object {
        /** These on, the rest off. */
        fun on(vararg permissions: Permission) = Grants(permissions.associateWith { Standing.ON })

        val ALL_ON = on(*Permission.entries.toTypedArray())
    }
}

/** Android's Accessibility settings, where its service is turned on, and off and on; openable from outside an Activity. */
fun accessibilitySettings(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

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
            Permission.ACCESSIBILITY -> context.startActivity(accessibilitySettings())
            Permission.OVERLAY -> context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
            Permission.NOTIFICATIONS -> notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/** The grants the system holds, read again as the screen comes back from the settings that give them and as the accessibility service starts or stops. */
class Permissions(private val context: Context, private val touch: TouchState) {
    private val _grants = MutableStateFlow(read())
    val grants: StateFlow<Grants> = _grants.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Main.immediate).launch { touch.hand.collect { refresh() } }
    }

    fun refresh() {
        _grants.value = read()
    }

    private fun read() = Grants(Permission.entries.associateWith { it.standing(context, touch) })
}
