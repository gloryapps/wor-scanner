package com.gloryapps.worscanner.windows.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.scanner.azhor.Send
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Card
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Lead
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Link
import com.gloryapps.worscanner.ui.Linked
import com.gloryapps.worscanner.ui.Standing
import com.gloryapps.worscanner.ui.Way
import com.gloryapps.worscanner.ui.resources.icon_folder
import com.gloryapps.worscanner.ui.resources.icon_restart_alt
import com.gloryapps.worscanner.ui.wayOf
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.account_none
import com.gloryapps.worscanner.windows.resources.account_scanned
import com.gloryapps.worscanner.windows.resources.game_closed
import com.gloryapps.worscanner.windows.resources.game_looking
import com.gloryapps.worscanner.windows.resources.game_not_windows
import com.gloryapps.worscanner.windows.resources.game_open
import com.gloryapps.worscanner.windows.resources.open_folder
import com.gloryapps.worscanner.windows.resources.scan_account
import com.gloryapps.worscanner.windows.resources.scan_again
import com.gloryapps.worscanner.windows.resources.scanning_account
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Desktop
import com.gloryapps.worscanner.ui.resources.Res as Shared

@Composable
internal fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.Open -> Desktop.getDesktop().open(effect.folder)
            }
        }
    }

    HomeScreen(state, viewModel::on, modifier)
}

@Composable
private fun HomeScreen(state: HomeState, onEvent: (HomeEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Colors.screen).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(said(state.game), style = Lettering.body, color = if (state.game is Game.Open) Colors.text else Colors.warning)
        Accented(
            label = stringResource(if (state.scanning) Res.string.scanning_account else Res.string.scan_account),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.scanning && state.game is Game.Open,
            onClick = { onEvent(HomeEvent.Scan) },
        )
        state.said?.let { Text(it, style = Lettering.data, color = Colors.muted) }
        Sending(state.account, state.linked, state.send, onEvent)
        if (state.kept != null) Link(stringResource(Res.string.open_folder)) { onEvent(HomeEvent.OpenKept) }
    }
}

/** The way to the lab, there before any scan: the link step while the scanner is not linked, and the newest account scanned on its way. */
@Composable
private fun Sending(account: AccountRead?, linked: Boolean, send: Send, onEvent: (HomeEvent) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val way = wayOf(
        send,
        code,
        onSend = { onEvent(HomeEvent.Send) },
        onLink = { onEvent(HomeEvent.Link(it)) },
        next = Way(stringResource(Res.string.scan_again), vectorResource(Shared.drawable.icon_restart_alt), null, { onEvent(HomeEvent.Scan) }),
        instead = Way(stringResource(Res.string.open_folder), vectorResource(Shared.drawable.icon_folder), null, { onEvent(HomeEvent.OpenKept) }),
        held = account != null,
    )

    Card(Modifier.fillMaxWidth(), leading = account != null) {
        if (account != null) {
            Text(stringResource(Res.string.account_scanned, account.heroes, account.gear, account.artifacts), style = Lettering.subtitle, color = Colors.text)
        } else {
            Text(stringResource(Res.string.account_none), style = Lettering.subtitle, color = Colors.muted)
        }
        if (linked) Linked(onUnlink = { onEvent(HomeEvent.Unlink) })
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
