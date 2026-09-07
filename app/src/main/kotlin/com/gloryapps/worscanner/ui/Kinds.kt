package com.gloryapps.worscanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.scanner.kind.Kind

/** The one place the app names a kind: what its button says. */
@Composable
fun Kind.label(): String = stringResource(
    when (this) {
        Kind.GEAR -> R.string.kind_gear
    },
)
