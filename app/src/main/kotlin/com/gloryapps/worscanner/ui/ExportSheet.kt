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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Outbound
import com.gloryapps.worscanner.capture.SharedFolder

/** What one press of export puts on its way out, each file under the name it lands by, and what the sheet says it holds. */
data class Outgoing(val name: String, val files: List<Outbound>, val holds: List<Pair<String, String>>)

/** Where an export landed, or why it did not. */
sealed interface Saved {
    data class Into(val folder: String, val files: Int) : Saved

    data class Failed(val why: String) : Saved
}

/** The sheet over whichever screen pressed export, wired to the doors out; nothing while nothing is on its way. */
@Composable
fun ExportSheet(export: ExportDelegate) {
    val outgoing by export.outgoing.collectAsStateWithLifecycle()
    val saved by export.saved.collectAsStateWithLifecycle()
    val shared by export.shared.collectAsStateWithLifecycle()
    val into by export.into.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val leaving = outgoing ?: return

    LaunchedEffect(saved) {
        val landed = saved as? Saved.Into ?: return@LaunchedEffect
        Toast.makeText(context, context.said(landed), Toast.LENGTH_LONG).show()
        export.forget()
    }

    ExportSheet(
        outgoing = leaving,
        failed = saved as? Saved.Failed,
        shared = shared,
        into = into,
        onChoose = export::choose,
        onShared = { export.toShared(leaving.files) },
        onShare = { context.startActivity(export.shareIntent(leaving.files)) },
        onClose = export::forget,
    )
}

/**
 * The one way out of the app, wherever export was pressed: the file that goes, what it holds, and
 * the two doors it can leave by. It closes once the file has landed, and stays open to say why it did not.
 */
@Composable
fun ExportSheet(
    outgoing: Outgoing,
    failed: Saved.Failed?,
    shared: List<SharedFolder>,
    into: SharedFolder?,
    onChoose: (SharedFolder) -> Unit,
    onShared: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
) {
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
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.export_close), tint = Colors.muted)
                }
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
                Landing(shared, into, onChoose)
                if (failed != null) Landed(failed)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accented(
                        stringResource(R.string.export_save),
                        Modifier.weight(1f),
                        said = into?.let { stringResource(R.string.export_to, stringResource(it.emulator.label)) },
                        enabled = into != null,
                        onClick = onShared,
                    )
                    Edged(stringResource(R.string.export_share), onClick = onShare)
                }
                Text(stringResource(R.string.export_note), style = Lettering.caption, color = Colors.muted)
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

private fun Context.said(landed: Saved.Into): String =
    resources.getQuantityString(R.plurals.export_saved, landed.files, landed.files) + "\n" + landed.folder

/** Why the file did not land, pinned beside the buttons so it is seen whether or not the sheet scrolls. */
@Composable
private fun Landed(failed: Saved.Failed) {
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
        Text(failed.why, style = Lettering.body, color = Colors.muted)
    }
}
