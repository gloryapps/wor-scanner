package com.gloryapps.worscanner.windows.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gloryapps.worscanner.scanner.azhor.Send
import com.gloryapps.worscanner.scanner.runs.Kept
import com.gloryapps.worscanner.ui.Card
import com.gloryapps.worscanner.ui.Lead
import com.gloryapps.worscanner.ui.Standing
import com.gloryapps.worscanner.ui.Way
import com.gloryapps.worscanner.ui.shown
import com.gloryapps.worscanner.ui.resources.icon_folder
import com.gloryapps.worscanner.ui.resources.icon_restart_alt
import com.gloryapps.worscanner.ui.resources.scan_else
import com.gloryapps.worscanner.ui.wayOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Link
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.ui.Segmented
import com.gloryapps.worscanner.ui.label
import com.gloryapps.worscanner.ui.resources.choose_kind
import com.gloryapps.worscanner.ui.resources.read_tile
import com.gloryapps.worscanner.ui.resources.scan
import com.gloryapps.worscanner.ui.resources.stop
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.game_closed
import com.gloryapps.worscanner.windows.resources.game_looking
import com.gloryapps.worscanner.windows.resources.game_not_windows
import com.gloryapps.worscanner.windows.resources.game_open
import com.gloryapps.worscanner.windows.resources.open_folder
import com.gloryapps.worscanner.windows.resources.reading
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Desktop
import com.gloryapps.worscanner.ui.resources.Res as Shared

@Composable
internal fun HomeScreen(onFront: () -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.Front -> onFront()
                is HomeEffect.Open -> Desktop.getDesktop().open(effect.folder)
            }
        }
    }

    HomeScreen(state, viewModel::on)
    state.over?.let { Overlay(it, state.progress, state.paused, onStop = { viewModel.on(HomeEvent.Scan) }) }
}

@Composable
private fun HomeScreen(state: HomeState, onEvent: (HomeEvent) -> Unit) {
    Column(Modifier.fillMaxSize().background(Colors.screen).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(said(state.game), style = Lettering.body, color = if (state.game is Game.Open) Colors.text else Colors.warning)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Section(stringResource(Shared.string.choose_kind))
            Segmented(Kind.entries.map { stringResource(it.label) }, Kind.entries.indexOf(state.kind), Modifier.fillMaxWidth()) {
                onEvent(HomeEvent.Choose(Kind.entries[it]))
            }
        }
        Accented(
            label = stringResource(if (state.scanning) Shared.string.stop else Shared.string.scan),
            modifier = Modifier.fillMaxWidth(),
            enabled = state.scanning || state.game is Game.Open,
            onClick = { onEvent(HomeEvent.Scan) },
        )
        Edged(stringResource(if (state.reading) Res.string.reading else Shared.string.read_tile), Modifier.fillMaxWidth()) { onEvent(HomeEvent.Read) }
        state.said?.let { said ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(said, style = Lettering.data, color = Colors.muted)
                if (state.kept != null) Link(stringResource(Res.string.open_folder)) { onEvent(HomeEvent.OpenKept) }
            }
        }
        state.scan?.let { scan -> Sending(scan, state.send, onEvent) }
    }
}

/** The newest scan kept, on its way to the lab: the link step where the scanner is not linked yet, and the one action on. */
@Composable
private fun Sending(scan: Kept.Scan, send: Send, onEvent: (HomeEvent) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val way = wayOf(
        send,
        code,
        onSend = { onEvent(HomeEvent.Send) },
        onLink = { onEvent(HomeEvent.LinkAndSend(it)) },
        next = Way(stringResource(Shared.string.scan_else), vectorResource(Shared.drawable.icon_restart_alt), null, { onEvent(HomeEvent.Next) }),
        instead = Way(stringResource(Res.string.open_folder), vectorResource(Shared.drawable.icon_folder), null, { onEvent(HomeEvent.OpenKept) }),
    )

    Card(Modifier.fillMaxWidth(), leading = true) {
        Text(listOfNotNull(scan.kind?.let { stringResource(it.label) }, scan.shown()).joinToString(" · "), style = Lettering.subtitle, color = Colors.text)
        Standing(send, code, { code = it }, onDone = { way.onClick?.takeIf { way.enabled }?.invoke() })
        Lead(way)
    }
}

@Composable
private fun said(game: Game): String = when (game) {
    Game.Looking -> stringResource(Res.string.game_looking)
    is Game.Open -> stringResource(Res.string.game_open, game.width, game.height)
    Game.Closed -> stringResource(Res.string.game_closed)
    Game.NotWindows -> stringResource(Res.string.game_not_windows)
}
