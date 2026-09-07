package com.gloryapps.worscanner.ui

import androidx.annotation.StringRes
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.scanner.kinds.Kind

/** The one place the app names a kind: what its button, its notification and its list entry call it. */
val Kind.label: Int
    @StringRes get() = when (this) {
        Kind.GEAR -> R.string.kind_gear
    }
