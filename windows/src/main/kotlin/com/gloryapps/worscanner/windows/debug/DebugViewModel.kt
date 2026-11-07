package com.gloryapps.worscanner.windows.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.scanner.account.Placed
import com.gloryapps.worscanner.scanner.account.Play
import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.scanner.runs.Accounts
import com.gloryapps.worscanner.scanner.runs.Reports
import com.gloryapps.worscanner.scanner.simulator.Rules
import com.gloryapps.worscanner.scanner.simulator.rulesAt
import com.gloryapps.worscanner.windows.game.GameWatch
import com.gloryapps.worscanner.windows.memory.GameMemory
import com.gloryapps.worscanner.windows.memory.clientOrNone
import com.gloryapps.worscanner.windows.memory.said
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.game_closed
import com.gloryapps.worscanner.windows.resources.squads_failed
import com.gloryapps.worscanner.windows.resources.squads_none
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

internal class DebugViewModel(private val watch: GameWatch, private val accounts: Accounts, private val reports: Reports) : ViewModel() {
    private val local = MutableStateFlow(DebugState())
    val state: StateFlow<DebugState> = local.asStateFlow()

    private val _effects = Channel<DebugEffect>(Channel.BUFFERED)
    val effects: Flow<DebugEffect> = _effects.receiveAsFlow()

    fun on(event: DebugEvent) {
        when (event) {
            DebugEvent.ReadSquads -> readSquads()
            is DebugEvent.Deploy -> play { it.copy(deployOrder = it.deployOrder.toggled(event.hero)) }
            is DebugEvent.Healer -> play { it.copy(healers = it.healers.toggled(event.hero)) }
            is DebugEvent.Inspiration -> play { it.copy(inspiration = it.inspiration.toggled(event.hero)) }
            is DebugEvent.Stance -> play { it.copy(stances = it.stances + (event.hero to event.stance)) }
            is DebugEvent.Ultimate -> play { it.copy(ultimates = it.ultimates + (event.hero to event.way)) }
            is DebugEvent.Tick -> play { it.copy(ticks = it.ticks + (event.hero to it.ticks[event.hero].orEmpty().toggled(event.tick))) }
            is DebugEvent.Place -> local.update { it.copy(placing = event.hero.takeIf { hero -> hero != it.placing }) }
            is DebugEvent.PutOn -> putOn(event.cell)
            is DebugEvent.Face -> play { play -> play.cells[event.hero]?.let { play.copy(cells = play.cells + (event.hero to it.copy(face = event.face))) } ?: play }
            DebugEvent.Save -> save()
            DebugEvent.OpenKept -> local.value.kept?.let { file -> viewModelScope.launch { _effects.send(DebugEffect.Open(file.parentFile)) } }
        }
    }

    /* A read anew keeps what was said of the heroes still in the squads, and one finding no squad screen keeps all of it; the simulator is asked again: it may have started since. */
    private fun readSquads() {
        if (local.value.reading) return
        local.update { it.copy(reading = true) }
        viewModelScope.launch {
            val game = watch.game.value
            val read = if (game == null) null else resultOf { GameMemory.of(game.handle).getOrThrow().use { accounts.squads(it, it.clientOrNone(reports)) } }
            read?.onFailure { reports.failed("reading the squads", it) }
            val kept = read?.getOrNull()
            val said = when {
                read == null -> getString(Res.string.game_closed)
                kept == null -> read.exceptionOrNull()?.said(Res.string.squads_failed) ?: getString(Res.string.squads_none)
                else -> null
            }
            val rules = rulesAt(SIMULATOR)
            local.update { state ->
                val heroes = kept?.squads?.heroes
                state.copy(
                    reading = false,
                    squads = kept?.squads ?: state.squads,
                    kept = kept?.file ?: state.kept,
                    rules = rules,
                    play = heroes?.let { state.play.of(it.toSet()).timed(it, rules) } ?: state.play,
                    saved = kept == null && state.saved,
                    said = said,
                )
            }
        }
    }

    private fun play(change: (Play) -> Play) = local.update { it.copy(play = change(it.play), saved = false) }

    /* A hero put where another stands takes his cell, and the other leaves the map; the next hero still off it is then the one placed. */
    private fun putOn(cell: List<Double>) = local.update { state ->
        val cells = state.play.cells
        val there = cells.entries.firstOrNull { it.value.cell == cell }?.key
        val hero = state.placing ?: return@update state.copy(placing = there)
        val moved = if (there == hero) cells - hero else cells.filterKeys { it != there } + (hero to Placed(cell, cells[hero]?.face ?: Placed(cell).face))
        val heroes = state.squads.orEmpty().heroes
        state.copy(
            play = state.play.copy(cells = moved),
            placing = if (there == hero) hero else heroes.firstOrNull { it !in moved } ?: hero,
            saved = false,
        )
    }

    private fun save() {
        val state = local.value
        val file = state.kept ?: return
        val squads = state.squads ?: return
        viewModelScope.launch {
            resultOf { accounts.keep(file, squads.map { it.copy(play = state.play) }) }
                .onSuccess { local.update { it.copy(saved = it.play == state.play) } }
                .onFailure { failure -> reports.failed("keeping the squads", failure); local.update { it.copy(said = failure.message ?: failure.toString()) } }
        }
    }

    private companion object {
        /** The Armory's simulator, where its `armory.ps1` starts it on the PC. */
        const val SIMULATOR = "http://127.0.0.1:5180"
    }
}

private fun List<String>.toggled(hero: String): List<String> = if (hero in this) this - hero else this + hero

private fun Play.of(heroes: Set<String>) = Play(
    deployOrder = deployOrder.filter { it in heroes },
    healers = healers.filter { it in heroes },
    inspiration = inspiration.filter { it in heroes },
    stances = stances.filterKeys { it in heroes },
    ultimates = ultimates.filterKeys { it in heroes },
    ticks = ticks.filterKeys { it in heroes },
    cells = cells.filterKeys { it in heroes },
)

/* A hero whose Ultimate the simulator times stays at its first way until the player picks another, as the simulator plays him; the file says so. */
private fun Play.timed(heroes: List<String>, rules: Rules?) =
    copy(ultimates = heroes.mapNotNull { hero -> (ultimates[hero] ?: rules?.ultimates?.get(hero)?.firstOrNull())?.let { hero to it } }.toMap())
