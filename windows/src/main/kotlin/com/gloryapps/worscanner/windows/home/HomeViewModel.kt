package com.gloryapps.worscanner.windows.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.scanner.azhor.Send
import com.gloryapps.worscanner.scanner.azhor.Sender
import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.scanner.runs.Accounts
import com.gloryapps.worscanner.scanner.runs.Reports
import com.gloryapps.worscanner.windows.game.Desktop
import com.gloryapps.worscanner.windows.game.GameWatch
import com.gloryapps.worscanner.windows.memory.GameMemory
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.account_empty
import com.gloryapps.worscanner.windows.resources.account_failed
import com.gloryapps.worscanner.windows.resources.account_refused
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

internal class HomeViewModel(
    private val watch: GameWatch,
    private val accounts: Accounts,
    private val sender: Sender,
    private val reports: Reports,
) : ViewModel() {
    private val game = if (Desktop.present) watch.game.map { window -> window?.let { Game.Open(it.client.width, it.client.height) } ?: Game.Closed } else flowOf(Game.NotWindows)
    private val local = MutableStateFlow(HomeState())

    val state: StateFlow<HomeState> = combine(game, sender.linked, local) { game, linked, local -> local.copy(game = game, linked = linked, send = local.send.standing(linked)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), HomeState())

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    fun on(event: HomeEvent) {
        when (event) {
            HomeEvent.Scan -> scan()
            HomeEvent.OpenKept -> local.value.kept?.let { folder -> viewModelScope.launch { _effects.send(HomeEffect.Open(folder)) } }
            HomeEvent.Send -> local.value.account?.let { account -> viewModelScope.launch { sender.send(account.scans, ::sending) } }
            is HomeEvent.Link -> viewModelScope.launch {
                val account = local.value.account
                if (account != null) sender.linkAndSend(event.code, account.scans, ::sending) else sender.link(event.code, ::sending)
            }
            HomeEvent.Unlink -> viewModelScope.launch { sender.unlink() }
        }
    }

    /* The link step stands while the scanner is not linked, whatever was last tried; linking anew leaves nothing tried. */
    private fun Send.standing(linked: Boolean): Send = when {
        !linked && this !is Send.Underway && this !is Send.Code -> Send.Code()
        linked && this is Send.Code -> Send.Idle
        else -> this
    }

    /* A game that holds no account yet, signed out or still loading, is a scan that failed: nothing in it is worth sending. */
    private fun scan() {
        val game = watch.game.value ?: return
        if (local.value.scanning) return
        local.update { it.copy(scanning = true) }
        viewModelScope.launch {
            val read = resultOf { GameMemory.of(game.handle).getOrThrow().use { accounts.read(it) } }.map(::AccountRead)
            read.onFailure { reports.failed("scanning the account", it) }
            val account = read.getOrNull()?.takeIf { it.heroes + it.gear + it.artifacts > 0 }
            val said = if (account != null) null else read.exceptionOrNull()?.said() ?: getString(Res.string.account_empty)
            local.update { it.copy(scanning = false, said = said, kept = read.getOrNull()?.scans?.firstOrNull()?.parentFile ?: it.kept, account = account ?: it.account, send = Send.Idle) }
        }
    }

    private suspend fun Throwable.said(): String =
        if (this is GameMemory.Refused) getString(Res.string.account_refused, code) else getString(Res.string.account_failed, message ?: toString())

    private fun sending(send: Send) = local.update { it.copy(send = send) }

    private companion object {
        const val STOP_AFTER_MS = 5_000L
    }
}
