package com.gloryapps.worscanner.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.gloryapps.worscanner.ui.earlier.EarlierScreen
import com.gloryapps.worscanner.ui.home.HomeScreen
import com.gloryapps.worscanner.ui.reading.ReadingScreen
import kotlinx.serialization.Serializable

/** Where a scan is started. */
@Serializable
data object Home : NavKey

/** Every reading kept on the device, newest first. */
@Serializable
data object Earlier : NavKey

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
            entry<Home> { HomeScreen(onEarlier = { backStack.add(Earlier) }) }
            entry<Earlier> { EarlierScreen(onReading = { backStack.add(Reading(it.stamp)) }, onBack = { backStack.removeLastOrNull() }) }
            entry<Reading> { ReadingScreen(it.stamp, onBack = { backStack.removeLastOrNull() }) }
        },
    )
}
