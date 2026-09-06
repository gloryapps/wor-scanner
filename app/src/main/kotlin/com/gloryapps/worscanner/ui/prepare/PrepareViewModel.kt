package com.gloryapps.worscanner.ui.prepare

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PrepareState(
    val accessibilityOn: Boolean = false,
)

class PrepareViewModel : ViewModel() {
    private val _state = MutableStateFlow(PrepareState())
    val state: StateFlow<PrepareState> = _state.asStateFlow()
}
