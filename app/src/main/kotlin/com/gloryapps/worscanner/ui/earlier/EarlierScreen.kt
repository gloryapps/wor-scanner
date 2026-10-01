package com.gloryapps.worscanner.ui.earlier

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.BuildConfig
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Confirm
import com.gloryapps.worscanner.ui.ExportSheet
import com.gloryapps.worscanner.ui.Inline
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Link
import com.gloryapps.worscanner.ui.Panel
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.said
import com.gloryapps.worscanner.ui.shown
import org.koin.compose.viewmodel.koinViewModel

/**
 * Every reading kept on the device, wired to the store, the export sheet and the stack. What it draws
 * is `Earlier`, which knows none of them.
 */
@Composable
internal fun EarlierScreen(onReading: (Kept) -> Unit, onBack: () -> Unit, viewModel: EarlierViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onReading)
    val back by rememberUpdatedState(onBack)

    LifecycleResumeEffect(Unit) {
        viewModel.returned()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EarlierEffect.NavigateBack -> back()
                is EarlierEffect.OpenReading -> open(effect.kept)
            }
        }
    }

    Earlier(state, viewModel::on)
    ExportSheet(viewModel.export)
}

/** The readings, newest first, each with the way out beside it; the scanner's link to the site under them. */
@Composable
internal fun Earlier(state: EarlierUiState, onEvent: (EarlierEvent) -> Unit) {
    Column(Modifier.fillMaxSize().background(Colors.screen).safeDrawingPadding()) {
        Header(state.readings.orEmpty().isNotEmpty(), onEvent)
        Rule()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.readings?.let { Readings(it, onEvent) }
            Site(state.linked, onEvent)
            Text(BuildConfig.VERSION_NAME, style = Lettering.dataSmall, color = Colors.faint)
        }
    }

    if (state.deleting.isNotEmpty()) {
        val one = state.deleting.size == 1
        Confirm(
            title = if (one) stringResource(R.string.earlier_delete_title) else stringResource(R.string.earlier_delete_all_title, state.deleting.size),
            said = stringResource(if (one) R.string.earlier_delete_said else R.string.earlier_delete_all_said),
            confirm = stringResource(if (one) R.string.earlier_delete_yes else R.string.earlier_delete_all_yes),
            onConfirm = { onEvent(EarlierEvent.ConfirmDelete) },
            onCancel = { onEvent(EarlierEvent.CancelDelete) },
        )
    }
}

/** What the screen is, the way back, and what can be done to every reading at once. */
@Composable
private fun Header(any: Boolean, onEvent: (EarlierEvent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onEvent(EarlierEvent.Back) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Colors.muted)
            }
            Text(stringResource(R.string.earlier_title), style = Lettering.title, color = Colors.text)
        }
        if (any) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Link(stringResource(R.string.earlier_delete_all), onClick = { onEvent(EarlierEvent.DeleteAll) })
                Link(stringResource(R.string.earlier_export_all), onClick = { onEvent(EarlierEvent.ExportAll) })
            }
        }
    }
}

/** Everything kept on the device, newest first, each with the way out beside it. */
@Composable
internal fun Readings(readings: List<Kept>, onEvent: (EarlierEvent) -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        if (readings.isEmpty()) {
            Text(stringResource(R.string.earlier_empty), Modifier.padding(16.dp), style = Lettering.body, color = Colors.muted)
        }
        readings.forEachIndexed { at, kept ->
            if (at > 0) Rule()
            Reading(kept, newest = at == 0, onEvent)
        }
        if (readings.isNotEmpty()) {
            Rule()
            Text(stringResource(R.string.earlier_note), Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = Lettering.caption, color = Colors.muted)
        }
    }
}

@Composable
private fun Reading(kept: Kept, newest: Boolean, onEvent: (EarlierEvent) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (newest) Colors.accentWash else Colors.raised)
            .clickable { onEvent(EarlierEvent.Open(kept)) }
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
            Link(stringResource(R.string.earlier_delete), onClick = { onEvent(EarlierEvent.Delete(kept)) })
            if (newest) {
                Inline(stringResource(R.string.earlier_export), accented = true, onClick = { onEvent(EarlierEvent.Export(kept)) })
            } else {
                Link(stringResource(R.string.earlier_export), onClick = { onEvent(EarlierEvent.Export(kept)) })
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(15.dp), tint = Colors.muted)
        }
    }
}

/** Whether scans can be sent to the site, and the way to undo it; the link itself is made from Home, the first time a scan is sent. */
@Composable
private fun Site(linked: Boolean, onEvent: (EarlierEvent) -> Unit) {
    if (linked) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.earlier_linked), style = Lettering.body, color = Colors.text)
            Inline(stringResource(R.string.earlier_unlink), onClick = { onEvent(EarlierEvent.Unlink) })
        }
    } else {
        Text(stringResource(R.string.earlier_unlinked), style = Lettering.body, color = Colors.muted)
    }
}
