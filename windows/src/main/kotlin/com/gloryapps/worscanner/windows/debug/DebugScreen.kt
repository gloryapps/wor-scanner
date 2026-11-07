package com.gloryapps.worscanner.windows.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gloryapps.worscanner.scanner.account.FACING_BOSS
import com.gloryapps.worscanner.scanner.account.Placed
import com.gloryapps.worscanner.scanner.account.Squads
import com.gloryapps.worscanner.scanner.account.called
import com.gloryapps.worscanner.scanner.simulator.Grid
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Link
import com.gloryapps.worscanner.ui.Pill
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.boss
import com.gloryapps.worscanner.windows.resources.deploy_order
import com.gloryapps.worscanner.windows.resources.deploy_order_said
import com.gloryapps.worscanner.windows.resources.facing
import com.gloryapps.worscanner.windows.resources.healers
import com.gloryapps.worscanner.windows.resources.inspiration
import com.gloryapps.worscanner.windows.resources.map_said
import com.gloryapps.worscanner.windows.resources.map_title
import com.gloryapps.worscanner.windows.resources.open_folder
import com.gloryapps.worscanner.windows.resources.read_squads
import com.gloryapps.worscanner.windows.resources.reading_squads
import com.gloryapps.worscanner.windows.resources.save_squads
import com.gloryapps.worscanner.windows.resources.simulator_missing
import com.gloryapps.worscanner.windows.resources.squad_line
import com.gloryapps.worscanner.windows.resources.squad_lord
import com.gloryapps.worscanner.windows.resources.squads_kept
import com.gloryapps.worscanner.windows.resources.squads_none
import com.gloryapps.worscanner.windows.resources.squads_saved
import com.gloryapps.worscanner.windows.resources.squads_section
import com.gloryapps.worscanner.windows.resources.squads_stage
import com.gloryapps.worscanner.windows.resources.plays
import com.gloryapps.worscanner.windows.resources.stance
import com.gloryapps.worscanner.windows.resources.ultimate
import com.gloryapps.worscanner.windows.resources.where_he_stands
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
 * The squads of the squad screen open in the game, read when asked, and what no screen holds asked of the player, each with only the
 * heroes and the ways the Armory's simulator offers: the supports, the deploy order where inspiration makes it count, each stance hero's
 * stance, each other's Ultimate timing and the ticks. Saved, the file holds it all, every piece in full, for a comp to be made of.
 */
@Composable
private fun Squads(state: DebugState, onEvent: (DebugEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(stringResource(Res.string.squads_section))
        Edged(stringResource(if (state.reading) Res.string.reading_squads else Res.string.read_squads)) { onEvent(DebugEvent.ReadSquads) }
        state.said?.let { Text(it, style = Lettering.data, color = Colors.muted) }
        if (state.squads?.isEmpty() == true) Text(stringResource(Res.string.squads_none), style = Lettering.caption, color = Colors.muted)
        state.squads.orEmpty().forEach { screen -> Read(screen) }
        if (state.squads?.isNotEmpty() == true && state.rules == null) Text(stringResource(Res.string.simulator_missing), style = Lettering.caption, color = Colors.warning)
    }
    val heroes = state.squads.orEmpty().flatMap { screen -> screen.squads.flatten().map { it.called } }.distinct()
    if (heroes.isEmpty()) return
    val rules = state.rules
    val play = state.play

    if (rules != null) {
        Heroes(stringResource(Res.string.healers), null, heroes.filter { it in rules.healers }, chosen = { it in play.healers }) { onEvent(DebugEvent.Healer(it)) }
        Heroes(stringResource(Res.string.inspiration), null, heroes.filter { it in rules.inspirers }, chosen = { it in play.inspiration }) { onEvent(DebugEvent.Inspiration(it)) }
        if (play.inspiration.isNotEmpty()) {
            Heroes(stringResource(Res.string.deploy_order), stringResource(Res.string.deploy_order_said), heroes, label = { hero ->
                play.deployOrder.indexOf(hero).takeIf { it >= 0 }?.let { "${it + 1} · $hero" } ?: hero
            }, chosen = { it in play.deployOrder }) { onEvent(DebugEvent.Deploy(it)) }
        }
        state.squads.orEmpty().firstNotNullOfOrNull { rules.gridOf(it.stageId) }?.let { grid -> Placing(grid, heroes, play.cells, state.placing, onEvent) }
        Plays(heroes.filter { hero -> listOf(rules.stances, rules.ultimates, rules.ticks).any { !it[hero].isNullOrEmpty() } }) { hero ->
            Ways(stringResource(Res.string.stance), rules.stances[hero], chosen = { play.stances[hero] == it }) { onEvent(DebugEvent.Stance(hero, it)) }
            Ways(stringResource(Res.string.ultimate), rules.ultimates[hero], chosen = { play.ultimates[hero] == it }) { onEvent(DebugEvent.Ultimate(hero, it)) }
            Ways(stringResource(Res.string.where_he_stands), rules.ticks[hero], chosen = { it in play.ticks[hero].orEmpty() }) { onEvent(DebugEvent.Tick(hero, it)) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        /* Saving again only writes the same file again; only a read under way holds it, its squads not yet the ones shown. */
        Accented(stringResource(Res.string.save_squads), Modifier.fillMaxWidth(), enabled = !state.reading) { onEvent(DebugEvent.Save) }
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

/** `heroes` as pills under `title`, chosen or not, a tap telling which; nothing where none is offered. */
@Composable
private fun Heroes(title: String, said: String?, heroes: List<String>, label: (String) -> String = { it }, chosen: (String) -> Boolean, onTap: (String) -> Unit) {
    if (heroes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(title)
        said?.let { Text(it, style = Lettering.caption, color = Colors.muted) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            heroes.forEach { hero -> Pill(label(hero), chosen = chosen(hero)) { onTap(hero) } }
        }
    }
}

/**
 * Where each hero stands on the stage's map: a hero picked, a tap on a cell puts him there and picks the next still off it; beneath, every
 * hero on the map with his facing. Cells are drawn as the simulator numbers them, the boss at the top; ground cells are the melee ones.
 */
@Composable
private fun Placing(grid: Grid, heroes: List<String>, cells: Map<String, Placed>, placing: String?, onEvent: (DebugEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(stringResource(Res.string.map_title))
        Text(stringResource(Res.string.map_said), style = Lettering.caption, color = Colors.muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            heroes.forEach { hero -> Pill(if (hero in cells) "$hero ${arrow(cells.getValue(hero).face)}" else hero, chosen = hero == placing) { onEvent(DebugEvent.Place(hero)) } }
        }
        Field(grid, cells, placing) { onEvent(DebugEvent.PutOn(it)) }
        heroes.filter { it in cells }.forEach { hero ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.facing, hero), style = Lettering.caption, color = Colors.muted, modifier = Modifier.width(FACING_LABEL))
                FACES.forEach { face -> Pill(arrow(face), chosen = cells.getValue(hero).face == face) { onEvent(DebugEvent.Face(hero, face)) } }
            }
        }
    }
}

/** Wide enough for a hero's name and "faces", so every hero's arrows line up. */
private val FACING_LABEL = 140.dp

/** The map's cells, each with the hero on it, three letters and his facing; the boss's cell marked. */
@Composable
private fun Field(grid: Grid, cells: Map<String, Placed>, placing: String?, onTap: (List<Double>) -> Unit) {
    val all = grid.melee.map { it to true } + grid.ranged.map { it to false }
    val xs = all.map { it.first[0] } + grid.boss[0]
    val ys = all.map { it.first[1] } + grid.boss[1]
    val left = xs.min()
    val top = ys.max()
    val at = { cell: List<Double> -> Modifier.offset(CELL * (cell[0] - left).toFloat(), CELL * (top - cell[1]).toFloat()).size(CELL).padding(2.dp) }
    Box(Modifier.size(CELL * (xs.max() - left + 1).toFloat(), CELL * (top - ys.min() + 1).toFloat())) {
        Box(at(grid.boss).clip(SHAPE).background(Colors.failureEdge), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.boss), style = Lettering.dataSmall, color = Colors.text)
        }
        all.forEach { (cell, ground) ->
            val there = cells.entries.firstOrNull { it.value.cell == cell }
            Box(
                at(cell).clip(SHAPE).background(if (ground) Colors.lifted else Colors.sunken)
                    .border(1.dp, if (there != null && there.key == placing) Colors.accent else Colors.edge, SHAPE)
                    .clickable { onTap(cell) },
                contentAlignment = Alignment.Center,
            ) {
                there?.let { Text("${it.key.take(3)}${arrow(it.value.face)}", style = Lettering.dataSmall, color = Colors.text) }
            }
        }
    }
}

/* The simulator's facings: 270 the boss, at the top; 0, 90 and 180 the turns its RotateDeltaByFaceType makes of a range, read as right, down and left. */
private val FACES = listOf(FACING_BOSS, 0, 90, 180)

private fun arrow(face: Int): String = when (face) {
    0 -> "→"
    90 -> "↓"
    180 -> "←"
    else -> "↑"
}

private val CELL = 44.dp
private val SHAPE = RoundedCornerShape(6.dp)

/** Each of `heroes` under his name, all his ways to play together; nothing where none has any. */
@Composable
private fun Plays(heroes: List<String>, ways: @Composable (String) -> Unit) {
    if (heroes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Section(stringResource(Res.string.plays))
        heroes.forEach { hero ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(hero, style = Lettering.stepName, color = Colors.text)
                ways(hero)
            }
        }
    }
}

/** One kind of a hero's ways, as pills under its name; nothing where the simulator offers him none. */
@Composable
private fun Ways(kind: String, ways: List<String>?, chosen: (String) -> Boolean, onTap: (String) -> Unit) {
    if (ways.isNullOrEmpty()) return
    Text(kind, style = Lettering.caption, color = Colors.muted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ways.forEach { way -> Pill(way, chosen = chosen(way)) { onTap(way) } }
    }
}
