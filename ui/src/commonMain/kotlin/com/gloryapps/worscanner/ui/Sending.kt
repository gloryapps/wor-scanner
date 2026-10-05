package com.gloryapps.worscanner.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.scanner.azhor.Linking
import com.gloryapps.worscanner.scanner.azhor.Send
import com.gloryapps.worscanner.scanner.azhor.Sending
import com.gloryapps.worscanner.ui.resources.Res
import com.gloryapps.worscanner.ui.resources.icon_check_circle
import com.gloryapps.worscanner.ui.resources.icon_error
import com.gloryapps.worscanner.ui.resources.icon_link
import com.gloryapps.worscanner.ui.resources.icon_link_off
import com.gloryapps.worscanner.ui.resources.icon_refresh
import com.gloryapps.worscanner.ui.resources.icon_send
import com.gloryapps.worscanner.ui.resources.icon_wifi_off
import com.gloryapps.worscanner.ui.resources.lab_refused
import com.gloryapps.worscanner.ui.resources.lab_refused_code
import com.gloryapps.worscanner.ui.resources.lab_refused_code_said
import com.gloryapps.worscanner.ui.resources.lab_refused_said
import com.gloryapps.worscanner.ui.resources.lab_sent
import com.gloryapps.worscanner.ui.resources.lab_sent_said
import com.gloryapps.worscanner.ui.resources.lab_too_large
import com.gloryapps.worscanner.ui.resources.lab_too_large_said
import com.gloryapps.worscanner.ui.resources.lab_unlinked
import com.gloryapps.worscanner.ui.resources.lab_unlinked_said
import com.gloryapps.worscanner.ui.resources.lab_unreachable
import com.gloryapps.worscanner.ui.resources.lab_unreachable_said
import com.gloryapps.worscanner.ui.resources.link_again
import com.gloryapps.worscanner.ui.resources.link_hint
import com.gloryapps.worscanner.ui.resources.link_lasts
import com.gloryapps.worscanner.ui.resources.link_once
import com.gloryapps.worscanner.ui.resources.link_send
import com.gloryapps.worscanner.ui.resources.link_send_hint
import com.gloryapps.worscanner.ui.resources.link_step_1
import com.gloryapps.worscanner.ui.resources.link_step_2
import com.gloryapps.worscanner.ui.resources.link_step_3
import com.gloryapps.worscanner.ui.resources.link_title
import com.gloryapps.worscanner.ui.resources.send
import com.gloryapps.worscanner.ui.resources.send_hint
import com.gloryapps.worscanner.ui.resources.sending
import com.gloryapps.worscanner.ui.resources.try_again
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** How sending stands, said above the action: the link step, or what the lab answered. */
@Composable
fun Standing(send: Send, code: String, onCode: (String) -> Unit, onDone: () -> Unit) {
    if (send is Send.Code) LinkStep(code, onCode, onDone)
    noticeOf(send)?.let { Told(it) }
}

/** The action that takes a scan further as sending stands, what is said under it, and whether it can be pressed yet. */
class Way(val label: String, val icon: ImageVector?, val hint: String?, val onClick: (() -> Unit)?, val enabled: Boolean = true)

/**
 * The way for each standing. `onLink` takes the code as typed; `next` follows a scan sent, and
 * `instead` keeps one the lab would not take, both as the platform has them.
 */
@Composable
fun wayOf(send: Send, code: String, onSend: () -> Unit, onLink: (String) -> Unit, next: Way, instead: Way): Way = when (send) {
    Send.Idle -> Way(stringResource(Res.string.send), vectorResource(Res.drawable.icon_send), stringResource(Res.string.send_hint), onSend)
    is Send.Code -> Way(stringResource(Res.string.link_send), vectorResource(Res.drawable.icon_link), stringResource(Res.string.link_send_hint), { onLink(dashed(code)) }, enabled = code.length == CODE)
    Send.Underway -> Way(stringResource(Res.string.sending), null, null, null, enabled = false)
    Send.Sent -> next
    is Send.Unsent -> when (send.why) {
        Sending.Unlinked -> Way(stringResource(Res.string.link_again), vectorResource(Res.drawable.icon_link), null, onSend)
        Sending.Unanswered, Sending.Sent -> Way(stringResource(Res.string.try_again), vectorResource(Res.drawable.icon_refresh), null, onSend)
        Sending.TooLarge, Sending.Refused -> instead
    }
}

/** The way drawn as the action a card leads with, what is said of it beneath. */
@Composable
fun Lead(way: Way, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Accented(way.label, Modifier.fillMaxWidth(), icon = way.icon, enabled = way.enabled, onClick = { way.onClick?.invoke() })
        way.hint?.let { Text(it, Modifier.fillMaxWidth(), style = Lettering.footnote, color = Colors.faint, textAlign = TextAlign.Center) }
    }
}

/** The scanner linked once, by the code the lab's Import a scan shows. */
@Composable
private fun LinkStep(code: String, onCode: (String) -> Unit, onDone: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            buildAnnotatedString {
                append(stringResource(Res.string.link_title))
                append(" ")
                withStyle(SpanStyle(color = Colors.muted, fontWeight = FontWeight.Normal)) { append(stringResource(Res.string.link_once)) }
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
            CodeField(code, onCode, stringResource(Res.string.link_hint), onDone = onDone)
            Text(stringResource(Res.string.link_lasts), style = Lettering.caption, color = Colors.faint)
        }
    }
}

/** What the lab answered, in a card edged by whether it went: the failure's colour, or the accent once it is sent. */
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
        Icon(vectorResource(notice.icon), contentDescription = null, Modifier.size(18.dp), tint = colour)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(notice.title), style = Lettering.stepName, color = Colors.text)
            Text(stringResource(notice.said), style = Lettering.caption, color = Colors.muted)
        }
    }
}

/** What the lab answered, as a card says it. */
private class Notice(val icon: DrawableResource, val title: StringResource, val said: StringResource, val failed: Boolean = true)

private fun noticeOf(send: Send): Notice? = when (send) {
    is Send.Code -> when (send.failed) {
        Linking.Refused -> Notice(Res.drawable.icon_error, Res.string.lab_refused_code, Res.string.lab_refused_code_said)
        Linking.Unanswered -> UNREACHABLE
        is Linking.Linked, null -> null
    }
    is Send.Unsent -> when (send.why) {
        Sending.Unlinked -> Notice(Res.drawable.icon_link_off, Res.string.lab_unlinked, Res.string.lab_unlinked_said)
        Sending.TooLarge -> Notice(Res.drawable.icon_error, Res.string.lab_too_large, Res.string.lab_too_large_said)
        Sending.Refused -> Notice(Res.drawable.icon_error, Res.string.lab_refused, Res.string.lab_refused_said)
        Sending.Unanswered, Sending.Sent -> UNREACHABLE
    }
    Send.Sent -> Notice(Res.drawable.icon_check_circle, Res.string.lab_sent, Res.string.lab_sent_said, failed = false)
    Send.Idle, Send.Underway -> null
}

private val UNREACHABLE = Notice(Res.drawable.icon_wifi_off, Res.string.lab_unreachable, Res.string.lab_unreachable_said)

private val LINK_STEPS = listOf(Res.string.link_step_1, Res.string.link_step_2, Res.string.link_step_3)
