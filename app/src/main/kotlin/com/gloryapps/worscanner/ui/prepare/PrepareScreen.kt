package com.gloryapps.worscanner.ui.prepare

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.core.net.toUri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.CaptureService
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PrepareScreen(onFollow: () -> Unit, viewModel: PrepareViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.returned()
        onPauseOrDispose { }
    }

    val projection = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it.data
        if (it.resultCode == Activity.RESULT_OK && data != null) {
            CaptureService.start(context, it.resultCode, data)
        }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.prepare_title), style = MaterialTheme.typography.headlineSmall)

            Step(
                label = stringResource(R.string.prepare_accessibility),
                done = state.accessibilityOn,
                action = stringResource(R.string.prepare_open_settings),
            ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }

            Step(
                label = stringResource(R.string.prepare_overlay),
                done = state.overlayAllowed,
                action = stringResource(R.string.prepare_open_settings),
            ) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
                )
            }

            Step(
                label = stringResource(R.string.prepare_capture),
                done = state.capturing,
                action = stringResource(R.string.prepare_allow),
            ) {
                projection.launch(context.getSystemService(MediaProjectionManager::class.java).wholeDisplayIntent())
            }

            Button(onClick = onFollow, enabled = state.ready) { Text(stringResource(R.string.prepare_follow)) }
        }
    }
}

/* Android 14 offers "one app" by default and Unity games are one app, but the walk reads the display. */
private fun MediaProjectionManager.wholeDisplayIntent(): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
    } else {
        createScreenCaptureIntent()
    }

@Composable
private fun Step(label: String, done: Boolean, action: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (done) "✓ $label" else label)
        Button(onClick = onAction, enabled = !done) { Text(action) }
    }
}
