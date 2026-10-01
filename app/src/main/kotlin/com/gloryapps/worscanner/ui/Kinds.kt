package com.gloryapps.worscanner.ui

import androidx.annotation.StringRes
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.scanner.kinds.Kind

/** The one place the app names a kind: what it is called, and how the player readies the game for its scan. */
private class Named(@StringRes val label: Int, val steps: List<Int>, @StringRes val note: Int)

private val Kind.named: Named
    get() = when (this) {
        Kind.GEAR -> Named(
            R.string.kind_gear,
            listOf(R.string.kind_gear_step_1, R.string.kind_gear_step_2, R.string.kind_gear_step_3),
            R.string.kind_gear_note,
        )
        Kind.HEROES -> Named(
            R.string.kind_heroes,
            listOf(R.string.kind_heroes_step_1, R.string.kind_heroes_step_2, R.string.kind_heroes_step_3),
            R.string.kind_heroes_note,
        )
        Kind.ARTIFACTS -> Named(
            R.string.kind_artifacts,
            listOf(R.string.kind_artifacts_step_1, R.string.kind_artifacts_step_2, R.string.kind_artifacts_step_3),
            R.string.kind_artifacts_note,
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
