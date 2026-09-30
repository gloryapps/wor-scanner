package com.gloryapps.worscanner.ui.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.gloryapps.worscanner.app.provided
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
import com.gloryapps.worscanner.ui.Field
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
import com.gloryapps.worscanner.update.Update
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
    /* Android 13 hides a notification the app was not let post, the scan's Stop with it: asked at Start, and the scan goes on either way. */
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        projection.launch(context.getSystemService(MediaProjectionManager::class.java).wholeDisplayIntent())
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.OpenAccessibilitySettings -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                HomeEffect.OpenOverlaySettings -> context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
                )
                HomeEffect.LaunchProjection -> if (context.mayNotNotify()) {
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    projection.launch(context.getSystemService(MediaProjectionManager::class.java).wholeDisplayIntent())
                }
                HomeEffect.StopCapture -> CaptureService.stop(context)
                is HomeEffect.OpenReading -> open(effect.kept)
                /* The system's installer asks the player to confirm, and the first time to let this app install others. */
                is HomeEffect.Install -> context.startActivity(
                    Intent(Intent.ACTION_VIEW).setDataAndType(context.provided(effect.apk), APK_TYPE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
                is HomeEffect.OpenPage -> context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
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
        Header(state.running, state.update, onEvent)
        BoxWithConstraints(Modifier.weight(1f)) {
            val start: @Composable () -> Unit = { Start(state, onEvent) }
            val kept: @Composable () -> Unit = { state.readings?.let { Readings(it, onEvent) } }

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

    if (state.deleting.isNotEmpty()) {
        val one = state.deleting.size == 1
        Confirm(
            title = if (one) stringResource(R.string.home_delete_title) else stringResource(R.string.home_delete_all_title, state.deleting.size),
            said = stringResource(if (one) R.string.home_delete_said else R.string.home_delete_all_said),
            confirm = stringResource(if (one) R.string.home_delete_yes else R.string.home_delete_all_yes),
            onConfirm = { onEvent(HomeEvent.ConfirmDelete) },
            onCancel = { onEvent(HomeEvent.CancelDelete) },
        )
    }
}

@Composable
private fun Header(running: ScanState.Running?, update: Update, onEvent: (HomeEvent) -> Unit) {
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
                when (update) {
                    is Update.Available -> Link(stringResource(R.string.home_update, update.release.version.toString())) { onEvent(HomeEvent.Update) }
                    is Update.Downloading -> Text(stringResource(R.string.home_update_downloading, update.release.version.toString()), style = Lettering.caption, color = Colors.muted)
                    is Update.Failed -> Link(stringResource(R.string.home_update_page, update.release.version.toString())) { onEvent(HomeEvent.Update) }
                    Update.None -> Unit
                }
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
internal fun Start(state: HomeUiState, onEvent: (HomeEvent) -> Unit) {
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
            Text(stringResource(state.kind.said), style = Lettering.body, color = Colors.muted)
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
        Text(stringResource(R.string.home_kinds), style = Lettering.caption, color = Colors.muted)
        Kind.entries.forEach { each ->
            Pill(stringResource(each.label), chosen = each == state.kind, onClick = { onEvent(HomeEvent.Choose(each)) })
        }
    }

    Smithy(state.site, onEvent)
}

/** The link to the site the scans are sent to: the code it showed typed here once, and the way to undo it. */
@Composable
private fun Smithy(site: SiteLink, onEvent: (HomeEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Section(stringResource(R.string.home_smithy))
        Card(Modifier.fillMaxWidth()) {
            if (site.linked) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_smithy_linked), Modifier.weight(1f), style = Lettering.body, color = Colors.text)
                    Inline(stringResource(R.string.home_smithy_unlink), onClick = { onEvent(HomeEvent.Unlink) })
                }
            } else {
                var code by rememberSaveable { mutableStateOf("") }
                val asked = { if (code.isNotBlank() && !site.asking) onEvent(HomeEvent.Link(code.trim())) }

                Text(stringResource(R.string.home_smithy_said), style = Lettering.body, color = Colors.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Field(code, { code = it }, stringResource(R.string.home_smithy_hint), Modifier.weight(1f), onDone = asked)
                    Edged(stringResource(if (site.asking) R.string.home_smithy_linking else R.string.home_smithy_link), onClick = asked)
                }
                site.refused?.let { Text(stringResource(it), style = Lettering.caption, color = Colors.warning) }
            }
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
            Text(stringResource(R.string.home_on), style = Lettering.caption, color = Colors.muted)
        } else {
            Inline(stringResource(R.string.home_grant), onClick = onGrant)
        }
    }
}

/** Everything kept on the device, newest first, each with the way out beside it. */
@Composable
internal fun Readings(readings: List<Kept>, onEvent: (HomeEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Section(stringResource(R.string.home_readings))
            if (readings.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Link(stringResource(R.string.home_delete_all), onClick = { onEvent(HomeEvent.DeleteAll) })
                    Link(stringResource(R.string.home_export_all), onClick = { onEvent(HomeEvent.ExportAll) })
                }
            }
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(kept.shown(), style = Lettering.subtitle, color = Colors.text)
            Text(kept.said(context), style = Lettering.caption, color = Colors.muted)
            (kept as? Kept.Scan)?.detail?.let { Text(it, style = Lettering.caption, color = Colors.warning, maxLines = 2) }
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

private fun Context.mayNotNotify(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/* Android 14 offers "one app" by default and Unity games are one app, but the scan reads the display. */
private fun MediaProjectionManager.wholeDisplayIntent(): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
    } else {
        createScreenCaptureIntent()
    }

private val WIDE = 720.dp
private const val APK_TYPE = "application/vnd.android.package-archive"
