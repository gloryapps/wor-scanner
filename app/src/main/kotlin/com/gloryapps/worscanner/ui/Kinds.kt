package com.gloryapps.worscanner.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.scanner.kinds.Kind

/** The one place the app names a kind: what it is called, and how the player readies the game for its scan. */
private class Named(@StringRes val label: Int, val steps: List<Int>, @StringRes val note: Int, @PluralsRes val pieces: Int, @StringRes val reminder: Int)

private val Kind.named: Named
    get() = when (this) {
        Kind.GEAR -> Named(
            R.string.kind_gear,
            listOf(R.string.kind_gear_step_1, R.string.kind_gear_step_2, R.string.kind_gear_step_3),
            R.string.kind_gear_note,
            R.plurals.kind_gear_pieces,
            R.string.kind_gear_reminder,
        )
        Kind.HEROES -> Named(
            R.string.kind_heroes,
            listOf(R.string.kind_heroes_step_1, R.string.kind_heroes_step_2, R.string.kind_heroes_step_3),
            R.string.kind_heroes_note,
            R.plurals.kind_heroes_pieces,
            R.string.kind_heroes_reminder,
        )
        Kind.ARTIFACTS -> Named(
            R.string.kind_artifacts,
            listOf(R.string.kind_artifacts_step_1, R.string.kind_artifacts_step_2, R.string.kind_artifacts_step_3),
            R.string.kind_artifacts_note,
            R.plurals.kind_artifacts_pieces,
            R.string.kind_artifacts_reminder,
        )
    }

/** What its button, its notification and its list entry call it. */
val Kind.label: Int
    @StringRes get() = named.label

/** What the player does in the game before pressing Scan, in order: string resources. */
val Kind.steps: List<Int>
    get() = named.steps

/** What the scan does once it runs, said under the steps. */
val Kind.note: Int
    @StringRes get() = named.note

/** The steps in one line, under the choice in the menu over the game. */
val Kind.reminder: Int
    @StringRes get() = named.reminder

/** What one tile of it is called, counted: `pieces read`. */
val Kind.pieces: Int
    @PluralsRes get() = named.pieces
