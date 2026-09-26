package com.gloryapps.worscanner.ui

import androidx.annotation.StringRes
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.scanner.kinds.Kind

/** The one place the app names a kind: what it is called, and how the player lays the game out for its scan. */
private class Named(@StringRes val label: Int, @StringRes val said: Int)

private val Kind.named: Named
    get() = when (this) {
        Kind.GEAR -> Named(R.string.kind_gear, R.string.home_scan_said_gear)
        Kind.HEROES -> Named(R.string.kind_heroes, R.string.home_scan_said_heroes)
        Kind.ARTIFACTS -> Named(R.string.kind_artifacts, R.string.home_scan_said_artifacts)
    }

/** What its button, its notification and its list entry call it. */
val Kind.label: Int
    @StringRes get() = named.label

/** What the home screen tells the player to open in the game before the scan. */
val Kind.said: Int
    @StringRes get() = named.said
