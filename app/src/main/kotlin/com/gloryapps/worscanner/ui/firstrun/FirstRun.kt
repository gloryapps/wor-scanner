package com.gloryapps.worscanner.ui.firstrun

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.gloryapps.worscanner.ui.Permissions
import com.gloryapps.worscanner.ui.canScan
import kotlinx.coroutines.flow.first

/** Whether the player has been through the first run, kept between runs in a DataStore of its own. */
class FirstRun(private val context: Context, private val permissions: Permissions) {

    /* An install from before the first run that already holds the grants a scan needs has no use for it, and never will. */
    suspend fun seen(): Boolean {
        context.firstRun.data.first()[SEEN]?.let { return it }
        permissions.refresh()
        val updated = permissions.granted.value.canScan
        if (updated) saw()
        return updated
    }

    suspend fun saw() {
        context.firstRun.edit { it[SEEN] = true }
    }

    private companion object {
        val SEEN = booleanPreferencesKey("seen")
    }
}

private val Context.firstRun by preferencesDataStore("first_run")
