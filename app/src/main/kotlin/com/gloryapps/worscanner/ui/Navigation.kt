package com.gloryapps.worscanner.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.gloryapps.worscanner.ui.follow.FollowScreen
import com.gloryapps.worscanner.ui.follow.ViewScreen
import com.gloryapps.worscanner.ui.prepare.PrepareScreen
import kotlinx.serialization.Serializable

/** Where the reader ties the game to the app: accessibility on, capture allowed. */
@Serializable
data object Prepare : NavKey

/** What was kept: readings and scans, to look at, share, save or delete. */
@Serializable
data object Follow : NavKey

/** One kept JSON, shown as written. */
@Serializable
data class View(val stamp: String) : NavKey

@Composable
fun Navigation() {
    val backStack = rememberNavBackStack(Prepare)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Prepare> { PrepareScreen(onFollow = { backStack.add(Follow) }) }
            entry<Follow> { FollowScreen(onView = { backStack.add(View(it.stamp)) }, onBack = { backStack.removeLastOrNull() }) }
            entry<View> { ViewScreen(it.stamp, onBack = { backStack.removeLastOrNull() }) }
        },
    )
}
