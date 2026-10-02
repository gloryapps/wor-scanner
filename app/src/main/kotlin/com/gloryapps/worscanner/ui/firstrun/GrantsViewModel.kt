package com.gloryapps.worscanner.ui.firstrun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.ui.Permissions
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class GrantsViewModel(private val permissions: Permissions, private val firstRun: FirstRun) : ViewModel() {
    private val _effects = Channel<GrantsEffect>(Channel.BUFFERED)
    val effects: Flow<GrantsEffect> = _effects.receiveAsFlow()

    val state: StateFlow<GrantsUiState> = permissions.grants
        .map { GrantsUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrantsUiState(permissions.grants.value))

    fun on(event: GrantsEvent) {
        when (event) {
            is GrantsEvent.Grant -> send(GrantsEffect.Grant(event.permission))
            GrantsEvent.Back -> send(GrantsEffect.NavigateBack)
            GrantsEvent.Finish -> viewModelScope.launch {
                firstRun.saw()
                _effects.send(GrantsEffect.Done)
            }
        }
    }

    fun returned() = permissions.refresh()

    private fun send(effect: GrantsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
