package com.gloryapps.worscanner.overlay

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gloryapps.worscanner.app.preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The corners of the safe band the capsule snaps to, and where the sheet then opens from. */
enum class Corner(val top: Boolean, val start: Boolean) {
    TOP_START(true, true),
    TOP_END(true, false),
    BOTTOM_START(false, true),
    BOTTOM_END(false, false),
}

/** Where the capsule was last let go, kept between sessions; null until it has been dragged once. */
class Parked(private val context: Context) {
    val corner: Flow<Corner?> = context.preferences.data.map { held ->
        held[CORNER]?.let { name -> Corner.entries.firstOrNull { it.name == name } }
    }

    suspend fun park(corner: Corner) {
        context.preferences.edit { it[CORNER] = corner.name }
    }

    private companion object {
        val CORNER = stringPreferencesKey("corner")
    }
}
