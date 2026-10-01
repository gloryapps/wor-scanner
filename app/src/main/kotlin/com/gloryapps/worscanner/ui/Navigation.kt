package com.gloryapps.worscanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.gloryapps.worscanner.ui.earlier.EarlierScreen
import com.gloryapps.worscanner.ui.firstrun.FirstRun
import com.gloryapps.worscanner.ui.firstrun.GrantsScreen
import com.gloryapps.worscanner.ui.firstrun.Intro
import com.gloryapps.worscanner.ui.home.HomeScreen
import com.gloryapps.worscanner.ui.reading.ReadingScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/** What the scanner does, the first time the app opens. */
@Serializable
data object Welcome : NavKey

/** The first run's grants, after `Welcome`. */
@Serializable
data object Setup : NavKey

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
fun Navigation(firstRun: FirstRun = koinInject()) {
    /* Nothing is drawn for the moment the store takes to answer: the launch window is the same colour. */
    val seen by produceState<Boolean?>(null) { value = firstRun.seen() }
    val start = seen?.let { if (it) Home else Welcome } ?: return
    val backStack = rememberNavBackStack(start)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Welcome> { Intro(onNext = { backStack.add(Setup) }) }
            entry<Setup> {
                GrantsScreen(onDone = {
                    backStack.clear()
                    backStack.add(Home)
                })
            }
            entry<Home> { HomeScreen(onEarlier = { backStack.add(Earlier) }) }
            entry<Earlier> { EarlierScreen(onReading = { backStack.add(Reading(it.stamp)) }, onBack = { backStack.removeLastOrNull() }) }
            entry<Reading> { ReadingScreen(it.stamp, onBack = { backStack.removeLastOrNull() }) }
        },
    )
}
