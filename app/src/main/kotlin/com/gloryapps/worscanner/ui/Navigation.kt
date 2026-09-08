package com.gloryapps.worscanner.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.gloryapps.worscanner.ui.home.HomeScreen
import com.gloryapps.worscanner.ui.reading.ReadingScreen
import kotlinx.serialization.Serializable

/** Where a scan is started and what it left is found. */
@Serializable
data object Home : NavKey

/** One reading, piece by piece. */
@Serializable
data class Reading(val stamp: String) : NavKey

@Composable
fun Navigation() {
    val backStack = rememberNavBackStack(Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> { HomeScreen(onReading = { backStack.add(Reading(it.stamp)) }) }
            entry<Reading> { ReadingScreen(it.stamp, onBack = { backStack.removeLastOrNull() }) }
        },
    )
}
