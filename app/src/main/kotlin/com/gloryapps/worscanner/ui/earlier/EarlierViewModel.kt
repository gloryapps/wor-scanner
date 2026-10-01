package com.gloryapps.worscanner.ui.earlier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.azhor.Link
import com.gloryapps.worscanner.azhor.Linking
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Readings
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import com.gloryapps.worscanner.ui.ExportDelegate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class EarlierViewModel(
    private val scanning: Scanning,
    private val readings: Readings,
    private val link: Link,
    val export: ExportDelegate,
) : ViewModel() {
    private val kept = MutableStateFlow<List<Kept>?>(null)
    private val site = MutableStateFlow(SiteLink())
    private val deleting = MutableStateFlow<List<Kept>>(emptyList())
    /** Whether the deletion asked about is every one, which also clears what interrupted scans left. */
    private var all = false
    private val _effects = Channel<EarlierEffect>(Channel.BUFFERED)
    val effects: Flow<EarlierEffect> = _effects.receiveAsFlow()

    init {
        /* A scan that ends while the screen is in front is listed without waiting for the next return. */
        viewModelScope.launch { scanning.state.filterIsInstance<ScanState.Ended>().collect { relist() } }
    }

    val state: StateFlow<EarlierUiState> = combine(
        kept,
        deleting,
        combine(site, link.linked) { row, linked -> row.copy(linked = linked) },
    ) { readings, deleting, site -> EarlierUiState(readings, deleting, site) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EarlierUiState())

    fun on(event: EarlierEvent) {
        when (event) {
            EarlierEvent.Back -> send(EarlierEffect.NavigateBack)
            is EarlierEvent.Open -> send(EarlierEffect.OpenReading(event.kept))
            is EarlierEvent.Export -> export.begin(event.kept)
            EarlierEvent.ExportAll -> export.begin(kept.value.orEmpty())
            is EarlierEvent.Delete -> ask(listOf(event.kept), all = false)
            EarlierEvent.DeleteAll -> ask(kept.value.orEmpty(), all = true)
            EarlierEvent.ConfirmDelete -> {
                val going = deleting.value
                val sweep = all && scanning.state.value !is ScanState.Running
                ask(emptyList(), all = false)
                viewModelScope.launch {
                    going.forEach { readings.delete(it) }
                    if (sweep) readings.sweep()
                    relist()
                }
            }
            EarlierEvent.CancelDelete -> ask(emptyList(), all = false)
            is EarlierEvent.Link -> viewModelScope.launch {
                site.value = SiteLink(asking = true)
                site.value = SiteLink(
                    refused = when (link.link(event.code)) {
                        is Linking.Linked -> null
                        Linking.Refused -> R.string.earlier_smithy_refused
                        Linking.Unanswered -> R.string.earlier_smithy_unanswered
                    },
                )
            }
            EarlierEvent.Unlink -> viewModelScope.launch { link.forget() }
        }
    }

    override fun onCleared() = export.clear()

    fun returned() {
        viewModelScope.launch { relist() }
    }

    private fun ask(readings: List<Kept>, all: Boolean) {
        this.all = all
        deleting.value = readings
    }

    private suspend fun relist() {
        kept.value = readings.list()
    }

    private fun send(effect: EarlierEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
