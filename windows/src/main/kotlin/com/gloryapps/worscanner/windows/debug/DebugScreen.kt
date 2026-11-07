package com.gloryapps.worscanner.windows.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.scanner.account.SquadHero
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.read_squads
import com.gloryapps.worscanner.windows.resources.reading_squads
import com.gloryapps.worscanner.windows.resources.squad_artifact
import com.gloryapps.worscanner.windows.resources.squad_hero_no_artifact
import com.gloryapps.worscanner.windows.resources.squad_hero_no_gear
import com.gloryapps.worscanner.windows.resources.squad_hero_worn
import com.gloryapps.worscanner.windows.resources.squad_lord
import com.gloryapps.worscanner.windows.resources.squad_title
import com.gloryapps.worscanner.windows.resources.squads_none
import com.gloryapps.worscanner.windows.resources.squads_section
import com.gloryapps.worscanner.windows.resources.squads_stage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** A dev build's Debug window: what only a developer needs, a section each. */
@Composable
internal fun DebugScreen(viewModel: DebugViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DebugScreen(state, viewModel::on)
}

@Composable
private fun DebugScreen(state: DebugState, onEvent: (DebugEvent) -> Unit) {
    Column(Modifier.fillMaxSize().background(Colors.screen).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Squads(state, onEvent)
    }
}

/** The squads of the squad screen open in the game, read when asked, each squad's heroes in their slots' order with the sets and artifact each wears. */
@Composable
private fun Squads(state: DebugState, onEvent: (DebugEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(stringResource(Res.string.squads_section))
        Edged(stringResource(if (state.reading) Res.string.reading_squads else Res.string.read_squads)) { onEvent(DebugEvent.ReadSquads) }
        state.said?.let { Text(it, style = Lettering.data, color = Colors.muted) }
        if (state.squads?.isEmpty() == true) Text(stringResource(Res.string.squads_none), style = Lettering.caption, color = Colors.muted)
        state.squads.orEmpty().forEach { screen ->
            Text(stringResource(Res.string.squads_stage, screen.stageId), style = Lettering.stepName, color = Colors.text)
            screen.squads.forEachIndexed { at, squad ->
                Text(stringResource(Res.string.squad_title, at + 1), style = Lettering.data, color = Colors.text)
                squad.forEach { Text(worn(it), style = Lettering.caption, color = Colors.muted) }
            }
        }
    }
}

/** A hero with how many pieces of each set he wears, and his artifact. */
@Composable
private fun worn(hero: SquadHero): String {
    val name = hero.name ?: "#${hero.heroId}"
    val sets = hero.pieces.groupingBy { it.set ?: "?" }.eachCount().entries.joinToString { (set, count) -> "$count $set" }
    val artifact = hero.artifact?.let { stringResource(Res.string.squad_artifact, it.name ?: "?", it.level ?: 0) }

    return stringResource(
        Res.string.squad_hero_worn,
        if (hero.lord) stringResource(Res.string.squad_lord, name) else name,
        sets.ifEmpty { stringResource(Res.string.squad_hero_no_gear) },
        artifact ?: stringResource(Res.string.squad_hero_no_artifact),
    )
}
