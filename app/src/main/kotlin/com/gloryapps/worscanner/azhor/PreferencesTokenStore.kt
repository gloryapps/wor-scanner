package com.gloryapps.worscanner.azhor

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gloryapps.worscanner.scanner.azhor.TokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The token in a DataStore of its own, private to the app and never backed up. */
class PreferencesTokenStore(private val context: Context) : TokenStore {
    override val token: Flow<String?> = context.link.data.map { it[TOKEN] }

    override suspend fun keep(token: String?) {
        context.link.edit { if (token == null) it.remove(TOKEN) else it[TOKEN] = token }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
    }
}

private val Context.link by preferencesDataStore("link")
