package com.gloryapps.worscanner.ui

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Outbound

/** What one press of export puts on its way out, each file under the name it lands by, and what the sheet says it holds. */
data class Outgoing(val name: String, val files: List<Outbound>, val holds: List<Pair<String, String>>)

/** Where an export landed, or why it did not. */
sealed interface Saved {
    data class Into(val folder: String, val files: Int) : Saved

    data class Failed(val why: String) : Saved
}

/** The sheet over whichever screen pressed export, wired to the doors out; nothing while nothing is on its way. */
@Composable
fun ExportSheet(exporting: Exporting) {
    val outgoing by exporting.outgoing.collectAsStateWithLifecycle()
    val saved by exporting.saved.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val leaving = outgoing ?: return

    ExportSheet(
        outgoing = leaving,
        saved = saved,
        onShared = { exporting.toShared(leaving.files) },
        onShare = { context.startActivity(exporting.shareIntent(leaving.files)) },
        onClose = exporting::forget,
    )
}

/**
 * The one way out of the app, wherever export was pressed: the file that goes, what it holds, and
 * the two doors it can leave by. It stays open after a save, saying where the file landed.
 */
@Composable
fun ExportSheet(
    outgoing: Outgoing,
    saved: Saved?,
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

                if (saved != null) Landed(saved)
            }
            Rule()

            Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accented(stringResource(R.string.export_shared), Modifier.weight(1f), onClick = onShared)
                    Edged(stringResource(R.string.export_share), onClick = onShare)
                }
                Text(stringResource(R.string.export_note), style = Lettering.caption, color = Colors.muted)
            }
        }
    }
}

/** Where the file went, said in the sheet rather than in a message that comes and goes. */
@Composable
private fun Landed(saved: Saved) {
    val failed = saved is Saved.Failed
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (failed) Colors.hairline else Colors.accentEdge, RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).border(1.dp, if (failed) Colors.warning else Colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (failed) Icons.Default.Close else Icons.Default.Check,
                contentDescription = null,
                Modifier.size(15.dp),
                tint = if (failed) Colors.warning else Colors.accent,
            )
        }
        when (saved) {
            is Saved.Into -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(pluralStringResource(R.plurals.export_saved, saved.files, saved.files), style = Lettering.body, color = Colors.text)
                Text(saved.folder, style = Lettering.dataSmall, color = Colors.muted)
            }
            is Saved.Failed -> Text(saved.why, style = Lettering.body, color = Colors.muted)
        }
    }
}
