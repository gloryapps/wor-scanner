package com.gloryapps.worscanner.windows.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

internal class DebugViewModel(private val watch: GameWatch, private val accounts: Accounts, private val reports: Reports) : ViewModel() {
    private val local = MutableStateFlow(DebugState())
    val state: StateFlow<DebugState> = local.asStateFlow()

    fun on(event: DebugEvent) {
        when (event) {
            DebugEvent.ReadSquads -> readSquads()
        }
    }

    private fun readSquads() {
        if (local.value.reading) return
        local.update { it.copy(reading = true) }
        viewModelScope.launch {
            val game = watch.game.value
            val read = if (game == null) null else resultOf { GameMemory.of(game.handle).getOrThrow().use { accounts.squads(it, it.clientOrNone(reports)) } }
            read?.onFailure { reports.failed("reading the squads", it) }
            val said = if (read == null) getString(Res.string.game_closed) else read.exceptionOrNull()?.said(Res.string.squads_failed)
            local.update { it.copy(reading = false, squads = read?.getOrNull() ?: it.squads, said = said) }
        }
    }
}
