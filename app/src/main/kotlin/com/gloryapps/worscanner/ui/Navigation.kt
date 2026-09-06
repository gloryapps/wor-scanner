package com.gloryapps.worscanner.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.gloryapps.worscanner.ui.follow.FollowScreen
import com.gloryapps.worscanner.ui.prepare.PrepareScreen
import kotlinx.serialization.Serializable

/** Where the reader ties the game to the app: accessibility on, capture allowed. */
@Serializable
data object Prepare : NavKey

/** A scan under way, or the last one's result. */
@Serializable
data object Follow : NavKey

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
            entry<Follow> { FollowScreen() }
        },
    )
}
