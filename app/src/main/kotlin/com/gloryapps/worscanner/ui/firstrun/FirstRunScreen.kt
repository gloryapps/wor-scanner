package com.gloryapps.worscanner.ui.firstrun

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.ScreenshotMonitor
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Back
import com.gloryapps.worscanner.ui.Brand
import com.gloryapps.worscanner.ui.Card
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.Grant
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Panel
import com.gloryapps.worscanner.ui.Permission
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.ui.Standing
import com.gloryapps.worscanner.ui.action
import com.gloryapps.worscanner.ui.rememberGrant
import org.koin.compose.viewmodel.koinViewModel

/** What the scanner does, in the four moves of a scan, before anything is asked of the player. */
@Composable
internal fun Intro(onNext: () -> Unit, onWatch: () -> Unit) {
    Column(PAGE, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Brand()
        Column(Modifier.widthIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.firstrun_title), style = Lettering.display, color = Colors.text)
            Text(stringResource(R.string.firstrun_lead), style = Lettering.body, color = Colors.muted)
        }
        Row(
            Modifier.weight(1f).fillMaxWidth().wrapContentHeight(Alignment.Top).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MOVES.forEachIndexed { at, move ->
                Move(at + 1, move, leading = at == MOVES.lastIndex, Modifier.weight(1f).fillMaxHeight())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Pages(at = 0)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Edged(stringResource(R.string.howto_watch), icon = Icons.Outlined.PlayCircle, onClick = onWatch)
                Accented(stringResource(R.string.firstrun_next), trailing = Icons.AutoMirrored.Outlined.ArrowForward, onClick = onNext)
            }
        }
    }
}

@Composable
private fun Move(number: Int, move: ScanMove, leading: Boolean, modifier: Modifier) {
    Card(modifier, leading = leading, padding = PaddingValues(10.dp)) {
        Image(
            painterResource(move.shot),
            contentDescription = null,
            Modifier.fillMaxWidth().aspectRatio(SHOT).clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$number", style = Lettering.data, color = Colors.accent)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(move.name), style = Lettering.stepName, color = Colors.text)
                Text(stringResource(move.said), style = Lettering.caption, color = Colors.muted)
            }
        }
    }
}

/** The first run's checklist, wired to the system's grants and to the store that ends it. What it draws is `Grants`. */
@Composable
internal fun GrantsScreen(onDone: () -> Unit, onBack: () -> Unit, viewModel: GrantsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val done by rememberUpdatedState(onDone)
    val back by rememberUpdatedState(onBack)
    val grant = rememberGrant(viewModel::returned)

    LifecycleResumeEffect(Unit) {
        viewModel.returned()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GrantsEffect.Grant -> grant(effect.permission)
                GrantsEffect.NavigateBack -> back()
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
    Row(PAGE, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Brand(Modifier.padding(bottom = 8.dp))
            Text(stringResource(R.string.firstrun_grants_title), style = Lettering.display, color = Colors.text)
            Text(stringResource(R.string.firstrun_grants_lead), style = Lettering.body, color = Colors.muted)
            Spacer(Modifier.weight(1f))
            Capture()
        }
        Column(Modifier.weight(1.35f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                    Asked(permission, state.grants[permission]) { onEvent(GrantsEvent.Grant(permission)) }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Back { onEvent(GrantsEvent.Back) }
                    Pages(at = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.left > 0) {
                        Text(
                            stringResource(R.string.firstrun_later),
                            Modifier.clickable { onEvent(GrantsEvent.Finish) }.padding(vertical = 12.dp),
                            style = Lettering.body,
                            color = Colors.muted,
                        )
                    }
                    Accented(stringResource(R.string.firstrun_done), enabled = state.left == 0, onClick = { onEvent(GrantsEvent.Finish) })
                }
            }
        }
    }
}

@Composable
private fun Asked(permission: Permission, standing: Standing, onGrant: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).background(Colors.sunken, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(permission.icon, contentDescription = null, Modifier.size(18.dp), tint = Colors.accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(permission.label), style = Lettering.stepName, color = Colors.text)
            if (standing == Standing.STALLED) {
                Text(stringResource(R.string.permission_stalled), style = Lettering.caption, color = Colors.warning)
            } else {
                Text(stringResource(permission.why), style = Lettering.caption, color = Colors.muted)
            }
        }
        val action = standing.action
        if (action == null) {
            Row(Modifier.width(84.dp), horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, Modifier.size(16.dp), tint = Colors.accent)
                Text(stringResource(R.string.permission_on), style = Lettering.body, color = Colors.accent)
            }
        } else {
            Grant(stringResource(action), onClick = onGrant)
        }
    }
}

/** The grant that is asked every time and so is not on the list: the screen's capture, as a scan starts. */
@Composable
private fun Capture() {
    val each = stringResource(R.string.firstrun_capture_each)
    val said = stringResource(R.string.firstrun_capture, each)
    val at = said.indexOf(each)

    Card(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.ScreenshotMonitor, contentDescription = null, Modifier.size(18.dp), tint = Colors.muted)
            Text(
                buildAnnotatedString {
                    append(said.take(at))
                    withStyle(SpanStyle(color = Colors.text)) { append(each) }
                    append(said.drop(at + each.length))
                },
                style = Lettering.caption,
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
            Box(Modifier.size(width = if (it == at) 20.dp else 6.dp, height = 6.dp).background(if (it == at) Colors.accent else Colors.edge, RoundedCornerShape(3.dp)))
        }
    }
}

/** A move of every scan, with the screenshot that shows it. */
private class ScanMove(@StringRes val name: Int, @StringRes val said: Int, @DrawableRes val shot: Int)

private val MOVES = listOf(
    ScanMove(R.string.firstrun_move_open, R.string.firstrun_move_open_said, R.drawable.firstrun_open),
    ScanMove(R.string.firstrun_move_scan, R.string.firstrun_move_scan_said, R.drawable.firstrun_scan),
    ScanMove(R.string.firstrun_move_send, R.string.firstrun_move_send_said, R.drawable.firstrun_send),
    ScanMove(R.string.firstrun_move_import, R.string.firstrun_move_import_said, R.drawable.firstrun_import),
)

/** A move's screenshot's width over its height, the shape the four are cut to. */
private const val SHOT = 1.65f

private const val PAGES = 2

private val PAGE = Modifier.fillMaxSize().background(Colors.screen).safeDrawingPadding().padding(start = 28.dp, top = 22.dp, end = 28.dp, bottom = 18.dp)
