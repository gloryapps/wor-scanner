package com.gloryapps.worscanner.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WifiOff
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.azhor.Linking
import com.gloryapps.worscanner.azhor.Sending
import com.gloryapps.worscanner.scan.Ended
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.CODE
import com.gloryapps.worscanner.ui.Card
import com.gloryapps.worscanner.ui.CodeField
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.Fonts
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Pill
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.ui.dashed
import com.gloryapps.worscanner.ui.ended
import com.gloryapps.worscanner.ui.label
import com.gloryapps.worscanner.ui.onPc
import com.gloryapps.worscanner.ui.pieces
import com.gloryapps.worscanner.ui.savingInto
import com.gloryapps.worscanner.ui.shown
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
    val way = wayOf(just, code)

    Card(modifier, leading = true, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        /* Wide, the action keeps to the card's foot, whole, and what is above it scrolls on a screen too short for it. */
        Column(
            if (wide) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Came(just)
            Rule()
            Standing(just.send, code, { code = it }, onDone = { way.event?.takeIf { way.enabled }?.let(onEvent) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Accented(
                stringResource(way.label),
                Modifier.fillMaxWidth(),
                icon = way.icon,
                enabled = way.enabled,
                onClick = { way.event?.let(onEvent) },
            )
            way.hint?.let { Text(it, Modifier.fillMaxWidth(), style = Lettering.footnote, color = Colors.faint, textAlign = TextAlign.Center) }
        }
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
                if (just.touched) stringResource(R.string.home_stopped_touched, count) else stringResource(R.string.home_stopped_because, just.scan.detail.orEmpty(), count),
                style = Lettering.caption,
                color = Colors.muted,
            )
        }
    }
}

/** How sending stands, said above the action: the link step, or what the site answered. */
@Composable
private fun Standing(send: Send, code: String, onCode: (String) -> Unit, onDone: () -> Unit) {
    if (send is Send.Code) LinkStep(code, onCode, onDone)
    noticeOf(send)?.let { Told(it) }
}

/** The scanner linked once, by the code the site's Import a scan shows. */
@Composable
private fun LinkStep(code: String, onCode: (String) -> Unit, onDone: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.home_link_title))
                append(" ")
                withStyle(SpanStyle(color = Colors.muted, fontWeight = FontWeight.Normal)) { append(stringResource(R.string.home_link_once)) }
            },
            style = Lettering.stepName,
            color = Colors.text,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LINK_STEPS.forEachIndexed { at, step ->
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = Fonts.mono, color = Colors.accent)) { append("${at + 1}") }
                        append("  ")
                        append(stringResource(step))
                    },
                    style = Lettering.caption,
                    color = Colors.muted,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CodeField(code, onCode, stringResource(R.string.home_link_hint), onDone = onDone)
            Text(stringResource(R.string.home_link_lasts), style = Lettering.caption, color = Colors.faint)
        }
    }
}

/** What the site answered, in a card edged by whether it went: the failure's colour, or the accent once it is sent. */
@Composable
private fun Told(notice: Notice) {
    val colour = if (notice.failed) Colors.failure else Colors.accent
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, if (notice.failed) Colors.failureEdge else Colors.accentEdge, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(notice.icon, contentDescription = null, Modifier.size(18.dp), tint = colour)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(notice.title), style = Lettering.stepName, color = Colors.text)
            Text(stringResource(notice.said), style = Lettering.caption, color = Colors.muted)
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
                Text(stringResource(R.string.home_scan_else), Modifier.weight(1f), style = Lettering.body, color = Colors.text)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, Modifier.size(15.dp), tint = Colors.muted)
            }
        }
    }
}

/** The action that takes the scan further as sending stands, what is said under it, and whether it can be pressed yet. */
private class Way(@StringRes val label: Int, val icon: ImageVector?, val hint: String?, val event: HomeEvent?, val enabled: Boolean = true)

@Composable
private fun wayOf(just: JustScanned, code: String): Way = when (val send = just.send) {
    Send.Idle -> Way(R.string.export_send, Icons.AutoMirrored.Outlined.Send, stringResource(R.string.home_send_hint), HomeEvent.Send)
    is Send.Code -> Way(
        R.string.home_link_send,
        Icons.Outlined.Link,
        stringResource(R.string.home_link_send_hint),
        HomeEvent.LinkAndSend(dashed(code)),
        enabled = code.length == CODE,
    )
    Send.Underway -> Way(R.string.export_sending, null, null, null, enabled = false)
    Send.Sent -> Way(R.string.home_scan_else, Icons.Outlined.RestartAlt, null, HomeEvent.Next)
    is Send.Unsent -> when (send.why) {
        Sending.Unlinked -> Way(R.string.home_link_again, Icons.Outlined.Link, null, HomeEvent.Send)
        Sending.Unanswered, Sending.Sent -> Way(R.string.home_try_again, Icons.Outlined.Refresh, null, HomeEvent.Send)
        /* Without an emulator's folder to save into, the file goes by Share. */
        Sending.TooLarge, Sending.Refused -> just.shared?.let { Way(R.string.home_save_instead, Icons.Outlined.Folder, stringResource(it.emulator.onPc), HomeEvent.Save) }
            ?: Way(R.string.home_share_instead, Icons.Outlined.Share, null, HomeEvent.Share)
    }
}

/** What the site answered, as a card says it. */
private class Notice(val icon: ImageVector, @StringRes val title: Int, @StringRes val said: Int, val failed: Boolean = true)

private fun noticeOf(send: Send): Notice? = when (send) {
    is Send.Code -> when (send.failed) {
        Linking.Refused -> Notice(Icons.Outlined.Error, R.string.home_refused_code, R.string.home_refused_code_said)
        Linking.Unanswered -> UNREACHABLE
        is Linking.Linked, null -> null
    }
    is Send.Unsent -> when (send.why) {
        Sending.Unlinked -> Notice(Icons.Outlined.LinkOff, R.string.home_unlinked, R.string.home_unlinked_said)
        Sending.TooLarge -> Notice(Icons.Outlined.Error, R.string.home_too_large, R.string.home_too_large_said)
        Sending.Refused -> Notice(Icons.Outlined.Error, R.string.home_refused, R.string.home_refused_said)
        Sending.Unanswered, Sending.Sent -> UNREACHABLE
    }
    Send.Sent -> Notice(Icons.Outlined.CheckCircle, R.string.home_sent, R.string.home_sent_said, failed = false)
    Send.Idle, Send.Underway -> null
}

private val UNREACHABLE = Notice(Icons.Outlined.WifiOff, R.string.home_unreachable, R.string.home_unreachable_said)

private val LINK_STEPS = listOf(R.string.home_link_step_1, R.string.home_link_step_2, R.string.home_link_step_3)
