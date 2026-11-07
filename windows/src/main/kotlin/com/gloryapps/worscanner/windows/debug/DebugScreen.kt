package com.gloryapps.worscanner.windows.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.scanner.account.Squads
import com.gloryapps.worscanner.scanner.account.called
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Link
import com.gloryapps.worscanner.ui.Pill
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.deploy_order
import com.gloryapps.worscanner.windows.resources.deploy_order_said
import com.gloryapps.worscanner.windows.resources.healers
import com.gloryapps.worscanner.windows.resources.inspiration
import com.gloryapps.worscanner.windows.resources.open_folder
import com.gloryapps.worscanner.windows.resources.play_words
import com.gloryapps.worscanner.windows.resources.play_words_said
import com.gloryapps.worscanner.windows.resources.read_squads
import com.gloryapps.worscanner.windows.resources.reading_squads
import com.gloryapps.worscanner.windows.resources.save_squads
import com.gloryapps.worscanner.windows.resources.squad_line
import com.gloryapps.worscanner.windows.resources.squad_lord
import com.gloryapps.worscanner.windows.resources.squads_kept
import com.gloryapps.worscanner.windows.resources.squads_none
import com.gloryapps.worscanner.windows.resources.squads_saved
import com.gloryapps.worscanner.windows.resources.squads_section
import com.gloryapps.worscanner.windows.resources.squads_stage
import com.gloryapps.worscanner.windows.resources.stance_hint
import com.gloryapps.worscanner.windows.resources.ultimate_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Desktop

/** A dev build's Debug window: what only a developer needs, a section each. */
@Composable
internal fun DebugScreen(viewModel: DebugViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DebugEffect.Open -> Desktop.getDesktop().open(effect.folder)
            }
        }
    }

    DebugScreen(state, viewModel::on)
}

@Composable
private fun DebugScreen(state: DebugState, onEvent: (DebugEvent) -> Unit) {
    Column(Modifier.fillMaxSize().background(Colors.screen).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Squads(state, onEvent)
    }
}

/**
 * The squads of the squad screen open in the game, read when asked, and what no screen holds asked of the player: whom he deploys in
 * which order, his supports, each hero's stance and Ultimate. Saved, the file holds it all, every piece in full, for a comp to be made of.
 */
@Composable
private fun Squads(state: DebugState, onEvent: (DebugEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(stringResource(Res.string.squads_section))
        Edged(stringResource(if (state.reading) Res.string.reading_squads else Res.string.read_squads)) { onEvent(DebugEvent.ReadSquads) }
        state.said?.let { Text(it, style = Lettering.data, color = Colors.muted) }
        if (state.squads?.isEmpty() == true) Text(stringResource(Res.string.squads_none), style = Lettering.caption, color = Colors.muted)
        state.squads.orEmpty().forEach { screen -> Read(screen) }
    }
    val heroes = state.squads.orEmpty().flatMap { screen -> screen.squads.flatten().map { it.called } }.distinct()
    if (heroes.isEmpty()) return

    Picks(stringResource(Res.string.deploy_order), stringResource(Res.string.deploy_order_said), heroes, label = { hero ->
        state.play.deployOrder.indexOf(hero).takeIf { it >= 0 }?.let { "${it + 1} · $hero" } ?: hero
    }, chosen = { it in state.play.deployOrder }) { onEvent(DebugEvent.Deploy(it)) }
    Picks(stringResource(Res.string.healers), null, heroes, chosen = { it in state.play.healers }) { onEvent(DebugEvent.Healer(it)) }
    Picks(stringResource(Res.string.inspiration), null, heroes, chosen = { it in state.play.inspiration }) { onEvent(DebugEvent.Inspiration(it)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(stringResource(Res.string.play_words))
        Text(stringResource(Res.string.play_words_said), style = Lettering.caption, color = Colors.muted)
        heroes.forEach { hero ->
            Text(hero, style = Lettering.stepName, color = Colors.text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Words(state.play.stances[hero].orEmpty(), stringResource(Res.string.stance_hint), Modifier.weight(1f)) { onEvent(DebugEvent.Stance(hero, it)) }
                Words(state.play.ultimates[hero].orEmpty(), stringResource(Res.string.ultimate_hint), Modifier.weight(1f)) { onEvent(DebugEvent.Ultimate(hero, it)) }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Accented(stringResource(Res.string.save_squads), Modifier.fillMaxWidth(), enabled = !state.saved) { onEvent(DebugEvent.Save) }
        state.kept?.let { kept ->
            Text(stringResource(if (state.saved) Res.string.squads_saved else Res.string.squads_kept, kept.name), style = Lettering.caption, color = Colors.muted)
            Link(stringResource(Res.string.open_folder)) { onEvent(DebugEvent.OpenKept) }
        }
    }
}

/** What a read found: the stage, and each squad's heroes in their slots' order. */
@Composable
private fun Read(screen: Squads) {
    Text(stringResource(Res.string.squads_stage, screen.stageId), style = Lettering.stepName, color = Colors.text)
    screen.squads.forEachIndexed { at, squad ->
        val heroes = squad.map { if (it.lord) stringResource(Res.string.squad_lord, it.called) else it.called }
        Text(stringResource(Res.string.squad_line, at + 1, heroes.joinToString(" · ")), style = Lettering.data, color = Colors.text)
    }
}

/** Every hero as a pill under `title`, chosen or not, a tap telling which. */
@Composable
private fun Picks(title: String, said: String?, heroes: List<String>, label: (String) -> String = { it }, chosen: (String) -> Boolean, onTap: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(title)
        said?.let { Text(it, style = Lettering.caption, color = Colors.muted) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            heroes.forEach { hero -> Pill(label(hero), chosen = chosen(hero)) { onTap(hero) } }
        }
    }
}

/** A line of the player's own words, `hint` while it is empty. */
@Composable
private fun Words(words: String, hint: String, modifier: Modifier = Modifier, onWords: (String) -> Unit) {
    BasicTextField(
        words,
        onWords,
        modifier,
        textStyle = Lettering.body.copy(color = Colors.text),
        singleLine = true,
        cursorBrush = SolidColor(Colors.accent),
        decorationBox = { field ->
            Box(
                Modifier.height(36.dp).clip(RoundedCornerShape(8.dp)).background(Colors.sunken).border(1.dp, Colors.edge, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (words.isEmpty()) Text(hint, style = Lettering.body, color = Colors.faint)
                field()
            }
        },
    )
}
