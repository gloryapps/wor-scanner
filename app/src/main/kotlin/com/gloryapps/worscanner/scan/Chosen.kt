package com.gloryapps.worscanner.scan

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gloryapps.worscanner.scanner.kinds.Kind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The kind the next scan will read, kept between runs, picked in the app or the overlay's menu.
 * A pick holds at once, so a Scan pressed right after it takes it; the store is written behind.
 */
class Chosen(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _kind = MutableStateFlow(FIRST)
    val kind: StateFlow<Kind> = _kind.asStateFlow()
    @Volatile private var picked = false

    init {
        scope.launch {
            val kept = context.preferences.data.first()[KIND]?.let { name -> Kind.entries.firstOrNull { it.name == name } }
            if (kept != null && !picked) _kind.value = kept
        }
    }

    fun choose(kind: Kind) {
        picked = true
        _kind.value = kind
        scope.launch { context.preferences.edit { it[KIND] = kind.name } }
    }

    companion object {
        /** What an unset store means. */
        val FIRST: Kind = Kind.entries.first()
        private val KIND = stringPreferencesKey("kind")
    }
}

private val Context.preferences by preferencesDataStore("scanner")
