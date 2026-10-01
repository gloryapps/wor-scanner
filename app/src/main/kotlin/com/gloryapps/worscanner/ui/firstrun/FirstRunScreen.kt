package com.gloryapps.worscanner.ui.firstrun

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ScreenshotMonitor
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Brand
import com.gloryapps.worscanner.ui.Card
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.FillingColumn
import com.gloryapps.worscanner.ui.Grant
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Panel
import com.gloryapps.worscanner.ui.Permission
import com.gloryapps.worscanner.ui.Reach
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.ui.rememberGrant
import org.koin.compose.viewmodel.koinViewModel

/** What the scanner does, in the four moves of a scan, before anything is asked of the player; Next stays in view while the rest scrolls. */
@Composable
internal fun Intro(onNext: () -> Unit) {
    Column(PAGE, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        FillingColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Brand()
            Column(Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.firstrun_title), style = Lettering.display, color = Colors.text)
                Text(stringResource(R.string.firstrun_lead), style = Lettering.lead, color = Colors.muted)
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MOVES.forEachIndexed { at, move ->
                    Move(at + 1, move, leading = at == MOVES.lastIndex, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Pages(at = 0)
            Accented(stringResource(R.string.firstrun_next), trailing = Icons.AutoMirrored.Outlined.ArrowForward, reach = Reach.LARGE, onClick = onNext)
        }
    }
}

/* Its screenshot is a slot until the pictures are taken: what it will show, written where it will be. */
@Composable
private fun Move(number: Int, move: ScanMove, leading: Boolean, modifier: Modifier) {
    Card(modifier, leading = leading, padding = PaddingValues(16.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Colors.sunken, RoundedCornerShape(8.dp)).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(move.shot), style = Lettering.dataSmall, color = Colors.faint, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$number", style = Lettering.numeral, color = Colors.accent)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(move.name), style = Lettering.step, color = Colors.text)
                Text(stringResource(move.said), style = Lettering.bodySmall, color = Colors.muted)
            }
        }
    }
}

/** The first run's checklist, wired to the system's grants and to the store that ends it. What it draws is `Grants`. */
@Composable
internal fun GrantsScreen(onDone: () -> Unit, viewModel: GrantsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val done by rememberUpdatedState(onDone)
    val grant = rememberGrant(viewModel::returned)

    LifecycleResumeEffect(Unit) {
        viewModel.returned()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GrantsEffect.Grant -> grant(effect.permission)
                GrantsEffect.Done -> done()
            }
        }
    }

    Grants(state, viewModel::on)
}

/**
 * The three grants, each with why it is asked and its way to be turned on, in any order. The
 * scanner is ready once all three are on; Later leaves the rest to the ask at Start.
 */
@Composable
internal fun Grants(state: GrantsUiState, onEvent: (GrantsEvent) -> Unit) {
    Row(PAGE, horizontalArrangement = Arrangement.spacedBy(48.dp)) {
        FillingColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Brand(Modifier.padding(bottom = 18.dp))
            Text(stringResource(R.string.firstrun_grants_title), style = Lettering.display, color = Colors.text)
            Text(stringResource(R.string.firstrun_grants_lead), style = Lettering.lead, color = Colors.muted)
            Spacer(Modifier.weight(1f))
            Capture()
        }
        Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FillingColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Section(stringResource(R.string.firstrun_permissions))
                    Text(
                        if (state.left == 0) stringResource(R.string.firstrun_all_set) else pluralStringResource(R.plurals.firstrun_left, state.left, state.left),
                        style = Lettering.dataSmall,
                        color = Colors.muted,
                    )
                }
                Panel(Modifier.fillMaxWidth()) {
                    Permission.entries.forEachIndexed { at, permission ->
                        if (at > 0) Rule()
                        Asked(permission, on = permission in state.granted) { onEvent(GrantsEvent.Grant(permission)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Pages(at = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.left > 0) {
                        Text(
                            stringResource(R.string.firstrun_later),
                            Modifier.clickable { onEvent(GrantsEvent.Finish) }.padding(vertical = 12.dp),
                            style = Lettering.body,
                            color = Colors.muted,
                        )
                    }
                    Accented(
                        stringResource(R.string.firstrun_done),
                        reach = Reach.LARGE,
                        enabled = state.left == 0,
                        onClick = { onEvent(GrantsEvent.Finish) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Asked(permission: Permission, on: Boolean, onGrant: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(Colors.sunken, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(permission.icon, contentDescription = null, Modifier.size(21.dp), tint = Colors.accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(permission.label), style = Lettering.subtitle, color = Colors.text)
            Text(stringResource(permission.why), style = Lettering.body, color = Colors.muted)
        }
        if (on) {
            Row(Modifier.width(110.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, Modifier.size(18.dp), tint = Colors.accent)
                Text(stringResource(R.string.permission_on), style = Lettering.body, color = Colors.accent)
            }
        } else {
            Grant(stringResource(R.string.permission_turn_on), onClick = onGrant)
        }
    }
}

/** The grant that is asked every time and so is not on the list: the screen's capture, as a scan starts. */
@Composable
private fun Capture() {
    val each = stringResource(R.string.firstrun_capture_each)
    val said = stringResource(R.string.firstrun_capture, each)
    val at = said.indexOf(each)

    Card(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.ScreenshotMonitor, contentDescription = null, Modifier.size(20.dp), tint = Colors.muted)
            Text(
                buildAnnotatedString {
                    append(said.take(at))
                    withStyle(SpanStyle(color = Colors.text)) { append(each) }
                    append(said.drop(at + each.length))
                },
                style = Lettering.body,
                color = Colors.muted,
            )
        }
    }
}

/** Which of the first run's two pages this is. */
@Composable
private fun Pages(at: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(PAGES) {
            Box(Modifier.size(width = if (it == at) 22.dp else 6.dp, height = 6.dp).background(if (it == at) Colors.accent else Colors.edge, RoundedCornerShape(3.dp)))
        }
    }
}

/** A move of every scan, with the screenshot that will show it. */
private class ScanMove(@StringRes val name: Int, @StringRes val said: Int, @StringRes val shot: Int)

private val MOVES = listOf(
    ScanMove(R.string.firstrun_move_pick, R.string.firstrun_move_pick_said, R.string.firstrun_move_pick_shot),
    ScanMove(R.string.firstrun_move_open, R.string.firstrun_move_open_said, R.string.firstrun_move_open_shot),
    ScanMove(R.string.firstrun_move_scan, R.string.firstrun_move_scan_said, R.string.firstrun_move_scan_shot),
    ScanMove(R.string.firstrun_move_send, R.string.firstrun_move_send_said, R.string.firstrun_move_send_shot),
)

private const val PAGES = 2

private val PAGE = Modifier.fillMaxSize().background(Colors.screen).safeDrawingPadding().padding(start = 56.dp, top = 48.dp, end = 56.dp, bottom = 36.dp)
