package com.gloryapps.worscanner.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gloryapps.worscanner.ui.resources.Res
import com.gloryapps.worscanner.ui.resources.send
import com.gloryapps.worscanner.ui.resources.sending
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.SharedFolder

/** The sheet over whichever screen pressed export, wired to its delegate; nothing while nothing is on its way. */
@Composable
fun ExportSheet(export: ExportDelegate) {
    val state by export.state.collectAsStateWithLifecycle()

    ExportEffects(export)
    ExportSheet(
        outgoing = state.outgoing ?: return,
        failed = state.failed,
        shared = state.shared,
        into = state.into,
        linked = state.linked,
        sending = state.sending,
        onChoose = { export.on(ExportEvent.Choose(it)) },
        onShared = { export.on(ExportEvent.Save) },
        onShare = { export.on(ExportEvent.Share) },
        onSend = { export.on(ExportEvent.Send) },
        onClose = { export.on(ExportEvent.Close) },
    )
}

/**
 * The one way out of the app, wherever export was pressed: the file that goes, what it holds, and
 * the doors it can leave by, the site first once the scanner is linked and a scan is among it. It
 * closes once the file has landed, and stays open to say why it did not.
 */
@Composable
fun ExportSheet(
    outgoing: Outgoing,
    failed: String?,
    shared: List<SharedFolder>,
    into: SharedFolder?,
    linked: Boolean,
    sending: Boolean,
    onChoose: (SharedFolder) -> Unit,
    onShared: () -> Unit,
    onShare: () -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
) {
    val sends = outgoing.forLab && linked

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .safeDrawingPadding()
                .widthIn(max = 520.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.raised)
                .border(1.dp, Colors.hairline, RoundedCornerShape(12.dp)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.export_title), style = Lettering.title, color = Colors.text)
                Close(onClose)
            }
            Rule()

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Section(stringResource(R.string.export_file))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Colors.sunken)
                            .border(1.dp, Colors.edge, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(outgoing.name, style = Lettering.data, color = Colors.text)
                        if (outgoing.files.size > 1) {
                            Text(stringResource(R.string.export_beside, outgoing.files.size - 1), style = Lettering.dataSmall, color = Colors.muted)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Section(stringResource(R.string.export_holds))
                    outgoing.holds.forEachIndexed { at, (label, value) ->
                        if (at > 0) Rule()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = Lettering.body, color = Colors.muted)
                            Text(value, style = Lettering.data, color = Colors.text)
                        }
                    }
                }
            }
            Rule()

            Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (sends) {
                    Accented(
                        stringResource(if (sending) Res.string.sending else Res.string.send),
                        Modifier.fillMaxWidth(),
                        enabled = !sending,
                        onClick = onSend,
                    )
                }
                Landing(shared, into, onChoose)
                if (failed != null) Landed(failed)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val said = into?.let { stringResource(R.string.export_to, stringResource(it.emulator.label)) }
                    /* The site leads once a scan can go there; saving to the folder steps back beside Share. */
                    if (sends) {
                        Edged(into?.let { savingInto(it) } ?: stringResource(R.string.export_save), Modifier.weight(1f), onClick = { if (into != null) onShared() })
                    } else {
                        Accented(stringResource(R.string.export_save), Modifier.weight(1f), said = said, enabled = into != null, onClick = onShared)
                    }
                    Edged(stringResource(R.string.export_share), onClick = onShare)
                }
                if (outgoing.forLab) Text(stringResource(R.string.export_note), style = Lettering.caption, color = Colors.muted)
                if (outgoing.forLab && !linked) Text(stringResource(R.string.export_send_link), style = Lettering.caption, color = Colors.muted)
            }
        }
    }
}

/** The folder the save lands in, kept beside the button that does it: where the PC shows it, and who to pick when there are two. */
@Composable
private fun Landing(shared: List<SharedFolder>, into: SharedFolder?, onChoose: (SharedFolder) -> Unit) {
    if (shared.isEmpty()) {
        Text(stringResource(R.string.export_no_shared), style = Lettering.body, color = Colors.muted)

        return
    }

    /* One emulator is said by the button alone; a second one is what makes the pills worth drawing. */
    if (shared.size > 1) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            shared.forEach { folder ->
                Pill(stringResource(folder.emulator.label), chosen = folder == into) { onChoose(folder) }
            }
        }
    }
    into?.let { Text(stringResource(it.emulator.onPc), style = Lettering.data, color = Colors.muted) }
}

/** What an export did outside the app, the other app's chooser or a toast, for a screen that exports with or without the sheet. */
@Composable
fun ExportEffects(export: ExportDelegate) {
    val context = LocalContext.current

    LaunchedEffect(export) {
        export.effects.collect { effect ->
            when (effect) {
                is ExportEffect.Share -> context.startActivity(effect.intent)
                is ExportEffect.Landed -> Toast.makeText(context, context.said(effect), Toast.LENGTH_LONG).show()
                is ExportEffect.Unlanded -> Toast.makeText(context, effect.why, Toast.LENGTH_LONG).show()
                is ExportEffect.Sent ->
                    Toast.makeText(context, context.resources.getQuantityString(R.plurals.export_sent, effect.scans, effect.scans), Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun Context.said(landed: ExportEffect.Landed): String =
    resources.getQuantityString(R.plurals.export_saved, landed.files, landed.files) + "\n" + landed.folder

/** Why the file did not land, pinned beside the buttons so it is seen whether or not the sheet scrolls. */
@Composable
private fun Landed(failed: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Colors.hairline, RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).border(1.dp, Colors.warning, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, contentDescription = null, Modifier.size(15.dp), tint = Colors.warning)
        }
        Text(failed, style = Lettering.body, color = Colors.muted)
    }
}
