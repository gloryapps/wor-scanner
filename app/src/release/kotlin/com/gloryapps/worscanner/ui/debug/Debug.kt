package com.gloryapps.worscanner.ui.debug

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/** A release has no way to the debug screen, so the header shows none. */
@Composable
internal fun DebugLink(onClick: () -> Unit) = Unit

/** A release has no debug screen; nothing leads here. */
@Composable
internal fun DebugScreen(onOpen: (NavKey) -> Unit, onBack: () -> Unit) = Unit
