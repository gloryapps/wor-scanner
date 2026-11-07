package com.gloryapps.worscanner.windows.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.scanner.account.Play
import com.gloryapps.worscanner.scanner.account.called
import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.scanner.runs.Accounts
import com.gloryapps.worscanner.scanner.runs.Reports
import com.gloryapps.worscanner.windows.game.GameWatch
import com.gloryapps.worscanner.windows.memory.GameMemory
import com.gloryapps.worscanner.windows.memory.clientOrNone
import com.gloryapps.worscanner.windows.memory.said
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.game_closed
import com.gloryapps.worscanner.windows.resources.squads_failed
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
            is DebugEvent.Stance -> play { it.copy(stances = it.stances.said(event.hero, event.words)) }
            is DebugEvent.Ultimate -> play { it.copy(ultimates = it.ultimates.said(event.hero, event.words)) }
            DebugEvent.Save -> save()
            DebugEvent.OpenKept -> local.value.kept?.let { file -> viewModelScope.launch { _effects.send(DebugEffect.Open(file.parentFile)) } }
        }
    }

    /* A read anew keeps what was said of the heroes still in the squads. */
    private fun readSquads() {
        if (local.value.reading) return
        local.update { it.copy(reading = true) }
        viewModelScope.launch {
            val game = watch.game.value
            val read = if (game == null) null else resultOf { GameMemory.of(game.handle).getOrThrow().use { accounts.squads(it, it.clientOrNone(reports)) } }
            read?.onFailure { reports.failed("reading the squads", it) }
            val said = if (read == null) getString(Res.string.game_closed) else read.exceptionOrNull()?.said(Res.string.squads_failed)
            val kept = read?.getOrNull()
            local.update { state ->
                val heroes = kept?.squads?.flatMap { screen -> screen.squads.flatten().map { it.called } }?.toSet()
                state.copy(
                    reading = false,
                    squads = kept?.squads ?: state.squads,
                    kept = kept?.file ?: state.kept,
                    play = heroes?.let(state.play::of) ?: state.play,
                    saved = kept == null && state.saved,
                    said = said,
                )
            }
        }
    }

    private fun play(change: (Play) -> Play) = local.update { it.copy(play = change(it.play), saved = false) }

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
}

private fun List<String>.toggled(hero: String): List<String> = if (hero in this) this - hero else this + hero

/* Words emptied unsay them: the hero plays as the game would. */
private fun Map<String, String>.said(hero: String, words: String): Map<String, String> = if (words.isBlank()) this - hero else this + (hero to words)

private fun Play.of(heroes: Set<String>) = Play(
    deployOrder = deployOrder.filter { it in heroes },
    healers = healers.filter { it in heroes },
    inspiration = inspiration.filter { it in heroes },
    stances = stances.filterKeys { it in heroes },
    ultimates = ultimates.filterKeys { it in heroes },
)
