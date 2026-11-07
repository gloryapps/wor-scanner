package com.gloryapps.worscanner.windows.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.gloryapps.worscanner.scanner.account.Play
import com.gloryapps.worscanner.scanner.account.Squads
import com.gloryapps.worscanner.scanner.account.called
import com.gloryapps.worscanner.scanner.simulator.Grid
import com.gloryapps.worscanner.scanner.simulator.Rules
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Card
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Link
import com.gloryapps.worscanner.ui.Pill
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.Section
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.benched
import com.gloryapps.worscanner.windows.resources.boss
import com.gloryapps.worscanner.windows.resources.card_no_cell
import com.gloryapps.worscanner.windows.resources.deploy_order
import com.gloryapps.worscanner.windows.resources.deploy_order_said
import com.gloryapps.worscanner.windows.resources.facing
import com.gloryapps.worscanner.windows.resources.healer
import com.gloryapps.worscanner.windows.resources.inspiration
import com.gloryapps.worscanner.windows.resources.lord
import com.gloryapps.worscanner.windows.resources.map_missing
import com.gloryapps.worscanner.windows.resources.map_said
import com.gloryapps.worscanner.windows.resources.map_title
import com.gloryapps.worscanner.windows.resources.no_cell
import com.gloryapps.worscanner.windows.resources.off_map
import com.gloryapps.worscanner.windows.resources.open_folder
import com.gloryapps.worscanner.windows.resources.read_squads
import com.gloryapps.worscanner.windows.resources.reading_squads
import com.gloryapps.worscanner.windows.resources.role
import com.gloryapps.worscanner.windows.resources.save_squads
import com.gloryapps.worscanner.windows.resources.simulator_missing
import com.gloryapps.worscanner.windows.resources.squad_line
import com.gloryapps.worscanner.windows.resources.squad_lord
import com.gloryapps.worscanner.windows.resources.squads_kept
import com.gloryapps.worscanner.windows.resources.squads_saved
import com.gloryapps.worscanner.windows.resources.squads_stage
import com.gloryapps.worscanner.windows.resources.stance
import com.gloryapps.worscanner.windows.resources.ultimate
import com.gloryapps.worscanner.windows.resources.where_he_stands
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Desktop

/** A dev build's Debug window: what only a developer needs. */
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

/**
 * The squads of the squad screen open in the game, read when asked, and what no screen holds asked of the player, each with only the
 * heroes and the ways the Armory's simulator offers. Reading and saving stay at the top; beneath, the map and the squads on the left, a
 * card for each hero with everything he is played with on the right. Saved, the file holds it all, every piece in full.
 */
@Composable
private fun DebugScreen(state: DebugState, onEvent: (DebugEvent) -> Unit) {
    Column(Modifier.fillMaxSize().background(Colors.screen)) {
        Top(state, onEvent)
        Rule()
        val squads = state.squads.orEmpty()
        val heroes = squads.heroes
        if (heroes.isEmpty()) return
        val lords = squads.flatMap { screen -> screen.squads.flatten().filter { it.lord }.map { it.called } }.toSet()
        val rules = state.rules
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(LEFT).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                squads.forEach { screen -> Read(screen) }
                if (rules != null) {
                    val grid = squads.firstNotNullOfOrNull { rules.gridOf(it.stageId) }
                    if (grid != null) {
                        Placing(grid, heroes, lords, state.play.cells, state.placing, onEvent)
                    } else {
                        Text(stringResource(Res.string.map_missing, squads.joinToString { it.stageId.toString() }), style = Lettering.caption, color = Colors.warning)
                    }
                }
                if (rules != null && state.play.inspiration.isNotEmpty()) Deploy(heroes, state.play, onEvent)
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                heroes.forEach { hero -> Hero(hero, hero in lords, rules, state.play, onEvent) }
            }
        }
    }
}

/** Reading and saving, and what came of the last of either. */
@Composable
private fun Top(state: DebugState, onEvent: (DebugEvent) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Edged(stringResource(if (state.reading) Res.string.reading_squads else Res.string.read_squads)) { onEvent(DebugEvent.ReadSquads) }
            /* Saving again only writes the same file again; only a read under way holds it, its squads not yet the ones shown. */
            if (state.squads != null) {
                Accented(stringResource(Res.string.save_squads), Modifier.width(SAVE), enabled = !state.reading) { onEvent(DebugEvent.Save) }
            }
            state.kept?.let { kept ->
                Text(stringResource(if (state.saved) Res.string.squads_saved else Res.string.squads_kept, kept.name), style = Lettering.caption, color = Colors.muted)
                Link(stringResource(Res.string.open_folder)) { onEvent(DebugEvent.OpenKept) }
            }
        }
        state.said?.let { Text(it, style = Lettering.data, color = Colors.muted) }
        if (state.squads != null && state.rules == null) Text(stringResource(Res.string.simulator_missing), style = Lettering.caption, color = Colors.warning)
    }
}

/** What a read found: the stage, and each squad's heroes in their slots' order. */
@Composable
private fun Read(screen: Squads) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.squads_stage, screen.stageId), style = Lettering.stepName, color = Colors.text)
        screen.squads.forEachIndexed { at, squad ->
            val heroes = squad.map { if (it.lord) stringResource(Res.string.squad_lord, it.called) else it.called }
            Text(stringResource(Res.string.squad_line, at + 1, heroes.joinToString(" · ")), style = Lettering.data, color = Colors.text)
        }
    }
}

/**
 * Where each hero stands on the stage's map: a hero picked, a tap on a cell puts him there and picks the next still off it. Who has no cell
 * yet is said beneath, a Lord left off the map as leading from the bench.
 */
@Composable
private fun Placing(grid: Grid, heroes: List<String>, lords: Set<String>, cells: Map<String, Placed>, placing: String?, onEvent: (DebugEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(stringResource(Res.string.map_title))
        Text(stringResource(Res.string.map_said), style = Lettering.caption, color = Colors.muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            heroes.forEach { hero ->
                val placed = cells[hero]
                Pill(if (placed != null) "$hero ${arrow(placed.face)}" else hero, chosen = hero == placing, warning = placed == null && hero !in lords) { onEvent(DebugEvent.Place(hero)) }
            }
        }
        Field(grid, cells, placing) { onEvent(DebugEvent.PutOn(it)) }
        val off = heroes.filter { it !in cells }
        val missing = off.filter { it !in lords }
        val bench = off.filter { it in lords }
        if (cells.isNotEmpty() && missing.isNotEmpty()) Text(stringResource(Res.string.no_cell, missing.joinToString()), style = Lettering.caption, color = Colors.warning)
        if (cells.isNotEmpty() && bench.isNotEmpty()) Text(stringResource(Res.string.benched, bench.joinToString()), style = Lettering.caption, color = Colors.muted)
    }
}

/** The deploy order, which only inspiration makes count: tapped in turn, the heroes whose place matters. */
@Composable
private fun Deploy(heroes: List<String>, play: Play, onEvent: (DebugEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section(stringResource(Res.string.deploy_order))
        Text(stringResource(Res.string.deploy_order_said), style = Lettering.caption, color = Colors.muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            heroes.forEach { hero ->
                val at = play.deployOrder.indexOf(hero)
                Pill(if (at >= 0) "${at + 1} · $hero" else hero, chosen = at >= 0) { onEvent(DebugEvent.Deploy(hero)) }
            }
        }
    }
}

/**
 * A hero's card: whether he leads, the support roles the simulator lets him play, his stance, his Ultimate's timing, his ticks, and where
 * he faces once on the map; nothing for what the simulator offers him none of.
 */
@Composable
private fun Hero(hero: String, lord: Boolean, rules: Rules?, play: Play, onEvent: (DebugEvent) -> Unit) {
    val placed = play.cells[hero]
    Card(Modifier.fillMaxWidth(), spacing = 6.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(hero, style = Lettering.stepName, color = Colors.text)
            if (lord) Text(stringResource(Res.string.lord), style = Lettering.caption, color = Colors.muted)
            if (placed == null && play.cells.isNotEmpty()) {
                Text(stringResource(if (lord) Res.string.off_map else Res.string.card_no_cell), style = Lettering.caption, color = if (lord) Colors.muted else Colors.warning)
            }
        }
        if (rules != null) {
            val roles = listOfNotNull(
                stringResource(Res.string.healer).takeIf { hero in rules.healers }?.let { it to (hero in play.healers) },
                stringResource(Res.string.inspiration).takeIf { hero in rules.inspirers }?.let { it to (hero in play.inspiration) },
            )
            if (roles.isNotEmpty()) {
                Text(stringResource(Res.string.role), style = Lettering.caption, color = Colors.muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.forEachIndexed { at, (role, chosen) ->
                        Pill(role, chosen = chosen) { onEvent(if (at == 0 && hero in rules.healers) DebugEvent.Healer(hero) else DebugEvent.Inspiration(hero)) }
                    }
                }
            }
            Ways(stringResource(Res.string.stance), rules.stances[hero], chosen = { play.stances[hero] == it }) { onEvent(DebugEvent.Stance(hero, it)) }
            Ways(stringResource(Res.string.ultimate), rules.ultimates[hero], chosen = { play.ultimates[hero] == it }) { onEvent(DebugEvent.Ultimate(hero, it)) }
            Ways(stringResource(Res.string.where_he_stands), rules.ticks[hero], chosen = { it in play.ticks[hero].orEmpty() }) { onEvent(DebugEvent.Tick(hero, it)) }
        }
        if (placed != null) {
            Text(stringResource(Res.string.facing), style = Lettering.caption, color = Colors.muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FACES.forEach { face -> Pill(arrow(face), chosen = placed.face == face) { onEvent(DebugEvent.Face(hero, face)) } }
            }
        }
    }
}

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
                there?.let { Text("${it.key.take(4)}\n${arrow(it.value.face)}", style = Lettering.dataSmall, color = Colors.text) }
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

/* The simulator's facings: 270 the boss, at the top; 0, 90 and 180 the turns its RotateDeltaByFaceType makes of a range, read as right, down and left. */
private val FACES = listOf(FACING_BOSS, 0, 90, 180)

private fun arrow(face: Int): String = when (face) {
    0 -> "→"
    90 -> "↓"
    180 -> "←"
    else -> "↑"
}

private val CELL = 56.dp
private val SHAPE = RoundedCornerShape(6.dp)

/** Wide enough for the map's seven columns and the page's margins. */
private val LEFT = 460.dp
private val SAVE = 180.dp
