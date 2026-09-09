package com.gloryapps.worscanner.scan

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gloryapps.worscanner.scanner.kinds.Kind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The kind the next scan will read, kept between runs. The app picks it; the overlay obeys. */
class Chosen(private val context: Context) {
    val kind: Flow<Kind> = context.preferences.data.map { held ->
        held[KIND]?.let { name -> Kind.entries.firstOrNull { it.name == name } } ?: FIRST
    }

    suspend fun choose(kind: Kind) {
        context.preferences.edit { it[KIND] = kind.name }
    }

    companion object {
        /** What a screen shows until the store has answered, and what an unset store means. */
        val FIRST: Kind = Kind.entries.first()
        private val KIND = stringPreferencesKey("kind")
    }
}

private val Context.preferences by preferencesDataStore("scanner")
