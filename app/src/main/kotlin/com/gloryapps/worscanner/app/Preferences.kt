package com.gloryapps.worscanner.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** The one store the app's preferences share; a second delegate on the same file is a crash. */
internal val Context.preferences: DataStore<Preferences> by preferencesDataStore("scanner")
