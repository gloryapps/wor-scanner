package com.gloryapps.worscanner.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gloryapps.worscanner.ui.resources.Res
import com.gloryapps.worscanner.ui.resources.scan_else
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.scanner.azhor.Send
import com.gloryapps.worscanner.scanner.runs.Ended
import com.gloryapps.worscanner.ui.Card
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.Lead
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Pill
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.ui.Standing
import com.gloryapps.worscanner.ui.Way
import com.gloryapps.worscanner.ui.ended
import com.gloryapps.worscanner.ui.label
import com.gloryapps.worscanner.ui.onPc
import com.gloryapps.worscanner.ui.pieces
import com.gloryapps.worscanner.ui.savingInto
import com.gloryapps.worscanner.ui.shown
import com.gloryapps.worscanner.ui.wayOf
import java.text.NumberFormat

/**
 * The scan that just ended, what it came to, and the ways it leaves: sent to the site, the scanner
 * linked first when it must be, or kept as a file. Side by side where the screen is wide enough.
 */
@Composable
internal fun Scanned(just: JustScanned, wide: Boolean, onEvent: (HomeEvent) -> Unit, modifier: Modifier = Modifier) {
    if (wide) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Outcome(just, wide = true, onEvent, Modifier.weight(1.45f).fillMaxHeight())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Keep(just, onEvent)
                Next(onEvent)
            }
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Outcome(just, wide = false, onEvent, Modifier.fillMaxWidth())
            Keep(just, onEvent)
            Next(onEvent)
        }
    }
}

/** What the scan came to and how sending it stands, over the one action that takes it further. */
@Composable
private fun Outcome(just: JustScanned, wide: Boolean, onEvent: (HomeEvent) -> Unit, modifier: Modifier) {
    var code by rememberSaveable { mutableStateOf("") }
    val way = wayOf(
        just.send,
        code,
        onSend = { onEvent(HomeEvent.Send) },
        onLink = { onEvent(HomeEvent.LinkAndSend(it)) },
        next = Way(stringResource(Res.string.scan_else), Icons.Outlined.RestartAlt, null, { onEvent(HomeEvent.Next) }),
        instead = instead(just, onEvent),
    )

    Card(modifier, leading = true, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        /* Wide, the action keeps to the card's foot, whole, and what is above it scrolls on a screen too short for it. */
        Column(
            if (wide) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Came(just)
            Rule()
            Standing(just.send, code, { code = it }, onDone = { way.onClick?.takeIf { way.enabled }?.invoke() })
        }
        Lead(way)
    }
}

/** The kind, how the scan ended and when, and how many it read; a stop says why. */
@Composable
private fun Came(just: JustScanned) {
    val context = LocalContext.current
    val stopped = just.scan.ended == Ended.STOPPED
    val read = just.scan.entries ?: 0
    val count = NumberFormat.getIntegerInstance().format(read)
    val pieces = pluralStringResource(just.kind.pieces, read)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Pill(stringResource(just.kind.label), chosen = true)
        Pill(just.scan.ended(context), warning = stopped)
        Spacer(Modifier.weight(1f))
        Text(just.scan.shown(), style = Lettering.dataSmall, color = Colors.muted)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(count, Modifier.alignByBaseline(), style = Lettering.count, color = Colors.text)
            Text(
                stringResource(if (stopped) R.string.home_read_stopped else R.string.home_read, pieces),
                Modifier.alignByBaseline(),
                style = Lettering.body,
                color = Colors.muted,
            )
        }
        if (stopped) {
            Text(
                if (just.byPlayer) stringResource(R.string.home_stopped_by_you, count) else stringResource(R.string.home_stopped_because, just.scan.detail.orEmpty(), count),
                style = Lettering.caption,
                color = Colors.muted,
            )
        }
    }
}

/** The file kept rather than sent: saved into the emulator's folder, or handed to another app. */
@Composable
private fun Keep(just: JustScanned, onEvent: (HomeEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Section(stringResource(R.string.home_keep))
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_keep_said), style = Lettering.caption, color = Colors.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                just.shared?.let { Edged(savingInto(it), Modifier.weight(1f), icon = Icons.Outlined.Folder, onClick = { onEvent(HomeEvent.Save) }) }
                Edged(stringResource(R.string.export_share), icon = Icons.Outlined.Share, onClick = { onEvent(HomeEvent.Share) })
            }
            just.shared?.let { Text(stringResource(it.emulator.onPc), style = Lettering.dataSmall, color = Colors.faint) }
                ?: Text(stringResource(R.string.export_no_shared), style = Lettering.body, color = Colors.muted)
        }
    }
}

@Composable
private fun Next(onEvent: (HomeEvent) -> Unit) {
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Section(stringResource(R.string.home_next))
        Card(
            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).clickable { onEvent(HomeEvent.Next) },
            padding = PaddingValues(horizontal = 14.dp),
        ) {
            Row(Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = null, Modifier.size(18.dp), tint = Colors.accent)
                Text(stringResource(Res.string.scan_else), Modifier.weight(1f), style = Lettering.body, color = Colors.text)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, Modifier.size(15.dp), tint = Colors.muted)
            }
        }
    }
}

/* Without an emulator's folder to save into, the file goes by Share. */
@Composable
private fun instead(just: JustScanned, onEvent: (HomeEvent) -> Unit): Way =
    just.shared?.let { Way(stringResource(R.string.home_save_instead), Icons.Outlined.Folder, stringResource(it.emulator.onPc), { onEvent(HomeEvent.Save) }) }
        ?: Way(stringResource(R.string.home_share_instead), Icons.Outlined.Share, null, { onEvent(HomeEvent.Share) })
