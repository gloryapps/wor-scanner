package com.gloryapps.worscanner.ui.home

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.BuildConfig
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.CaptureService
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Card
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Confirm
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.ExportSheet
import com.gloryapps.worscanner.ui.Inline
import com.gloryapps.worscanner.ui.Link
import com.gloryapps.worscanner.ui.Panel
import com.gloryapps.worscanner.ui.Pill
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.ui.said
import com.gloryapps.worscanner.ui.shown
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.label
import org.koin.compose.viewmodel.koinViewModel

/**
 * Where a scan is started and what it left is found, wired to the service, the system and the store.
 * What it draws is `Home`, which knows none of them.
 */
@Composable
internal fun HomeScreen(onReading: (Kept) -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val open by rememberUpdatedState(onReading)

    LifecycleResumeEffect(Unit) {
        viewModel.returned()
        onPauseOrDispose { }
    }

    val projection = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it.data
        if (it.resultCode == Activity.RESULT_OK && data != null) CaptureService.start(context, it.resultCode, data)
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.OpenAccessibilitySettings -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                HomeEffect.OpenOverlaySettings -> context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
                )
                HomeEffect.LaunchProjection -> projection.launch(context.getSystemService(MediaProjectionManager::class.java).wholeDisplayIntent())
                HomeEffect.StopCapture -> CaptureService.stop(context)
                is HomeEffect.OpenReading -> open(effect.kept)
            }
        }
    }

    Home(state, viewModel::on)
    ExportSheet(viewModel.export)
}

/**
 * The grants and the scan on one side, every reading kept on the other. Two columns where the
 * screen is wide enough for them, one where it is not.
 */
@Composable
internal fun Home(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    Column(Modifier.fillMaxSize().background(Colors.screen).safeDrawingPadding()) {
        Header(state.running)
        BoxWithConstraints(Modifier.weight(1f)) {
            val start: @Composable () -> Unit = { Start(state, onEvent) }
            val kept: @Composable () -> Unit = { Readings(state.readings, onEvent) }

            if (maxWidth >= WIDE) {
                Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 22.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1.15f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) { start() }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) { kept() }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    start()
                    kept()
                }
            }
        }
    }

    if (state.deleting != null) {
        Confirm(
            title = stringResource(R.string.home_delete_title),
            said = stringResource(R.string.home_delete_said),
            confirm = stringResource(R.string.home_delete_yes),
            onConfirm = { onEvent(HomeEvent.ConfirmDelete) },
            onCancel = { onEvent(HomeEvent.CancelDelete) },
        )
    }
}

@Composable
private fun Header(running: ScanState.Running?) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(9.dp).background(Colors.accent, CircleShape))
                Text(stringResource(R.string.app_name), style = Lettering.brand, color = Colors.text)
                Text(BuildConfig.VERSION_NAME, style = Lettering.dataSmall, color = Colors.muted)
            }
            running?.let {
                Text(
                    "${stringResource(it.kind.label)} ${it.progress.done}/${it.progress.held}",
                    style = Lettering.dataSmall,
                    color = Colors.muted,
                )
            }
        }
        Rule()
    }
}

/** The grants, the scan and the kind it will read: everything that happens before a scan runs. */
@Composable
private fun Start(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Section(stringResource(R.string.home_permissions))
        Card(Modifier.fillMaxWidth()) {
            Given(stringResource(R.string.home_accessibility), state.accessibilityOn) { onEvent(HomeEvent.GrantAccessibility) }
            Rule()
            Given(stringResource(R.string.home_overlay), state.overlayAllowed) { onEvent(HomeEvent.GrantOverlay) }
        }
    }

    Card(Modifier.fillMaxWidth(), leading = true) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.home_scan_title, stringResource(state.kind.label)), style = Lettering.title, color = Colors.text)
            Text(stringResource(R.string.home_scan_said, stringResource(state.kind.label)), style = Lettering.body, color = Colors.muted)
        }
        if (state.capturing) {
            Text(stringResource(R.string.home_capturing), style = Lettering.body, color = Colors.accent)
            Edged(stringResource(R.string.home_stop), onClick = { onEvent(HomeEvent.Stop) })
        } else {
            Accented(
                stringResource(R.string.home_start),
                said = stringResource(R.string.home_to_game),
                enabled = state.ready,
                onClick = { onEvent(HomeEvent.Start) },
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.home_kinds), style = Lettering.dataSmall, color = Colors.muted)
        Kind.entries.forEach { each ->
            Pill(stringResource(each.label), chosen = each == state.kind, onClick = { onEvent(HomeEvent.Choose(each)) })
        }
    }
}

@Composable
private fun Given(label: String, given: Boolean, onGrant: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (given) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                Modifier.size(14.dp),
                tint = if (given) Colors.accent else Colors.warning,
            )
            Text(label, style = Lettering.body, color = if (given) Colors.text else Colors.muted)
        }
        if (given) {
            Text(stringResource(R.string.home_on), style = Lettering.dataSmall, color = Colors.muted)
        } else {
            Inline(stringResource(R.string.home_grant), onClick = onGrant)
        }
    }
}

/** Everything kept on the device, newest first, each with the way out beside it. */
@Composable
private fun Readings(readings: List<Kept>, onEvent: (HomeEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Section(stringResource(R.string.home_readings))
            if (readings.isNotEmpty()) Link(stringResource(R.string.home_export_all), onClick = { onEvent(HomeEvent.ExportAll) })
        }

        Panel(Modifier.fillMaxWidth()) {
            if (readings.isEmpty()) {
                Text(stringResource(R.string.home_empty), Modifier.padding(16.dp), style = Lettering.body, color = Colors.muted)
            }
            readings.forEachIndexed { at, kept ->
                if (at > 0) Rule()
                Reading(kept, newest = at == 0, onEvent)
            }
            if (readings.isNotEmpty()) {
                Rule()
                Text(stringResource(R.string.home_note), Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = Lettering.caption, color = Colors.muted)
            }
        }
    }
}

@Composable
private fun Reading(kept: Kept, newest: Boolean, onEvent: (HomeEvent) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (newest) Colors.accentWash else Colors.raised)
            .clickable { onEvent(HomeEvent.Open(kept)) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(kept.shown(), style = Lettering.data, color = Colors.text)
            Text(kept.said(context), style = Lettering.caption, color = Colors.muted)
            kept.detail?.let { Text(it, style = Lettering.caption, color = Colors.warning, maxLines = 2) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Link(stringResource(R.string.home_delete), onClick = { onEvent(HomeEvent.Delete(kept)) })
            if (newest) {
                Inline(stringResource(R.string.home_export), accented = true, onClick = { onEvent(HomeEvent.Export(kept)) })
            } else {
                Link(stringResource(R.string.home_export), onClick = { onEvent(HomeEvent.Export(kept)) })
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(15.dp), tint = Colors.muted)
        }
    }
}

/* Android 14 offers "one app" by default and Unity games are one app, but the scan reads the display. */
private fun MediaProjectionManager.wholeDisplayIntent(): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
    } else {
        createScreenCaptureIntent()
    }

private val WIDE = 720.dp
