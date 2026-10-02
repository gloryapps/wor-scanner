package com.gloryapps.worscanner.ui.debug

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.ui.Back
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Edged
import com.gloryapps.worscanner.ui.Home
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Panel
import com.gloryapps.worscanner.ui.Rule
import com.gloryapps.worscanner.ui.Setup
import com.gloryapps.worscanner.ui.Welcome
import com.gloryapps.worscanner.ui.home.Stage

/** The way to the debug screen, in Home's header; a debug build's only. */
@Composable
internal fun DebugLink(onClick: () -> Unit) {
    Row(
        Modifier.height(44.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.BugReport, contentDescription = null, Modifier.size(18.dp), tint = Colors.warning)
        Text(stringResource(R.string.debug_title), style = Lettering.body, color = Colors.warning)
    }
}

/** The screens a player meets once or only by playing, each opened as it runs, over this one; back returns here. */
@Composable
internal fun DebugScreen(onOpen: (NavKey) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Colors.screen).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Back(onBack)
            Text(stringResource(R.string.debug_title), style = Lettering.title, color = Colors.text)
        }
        Rule()
        Panel(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 14.dp).fillMaxWidth()) {
            SCREENS.forEachIndexed { at, screen ->
                if (at > 0) Rule()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(stringResource(screen.name), style = Lettering.stepName, color = Colors.text)
                        Text(stringResource(screen.said), style = Lettering.caption, color = Colors.muted)
                    }
                    Edged(stringResource(R.string.debug_open), onClick = { onOpen(screen.key) })
                }
            }
        }
    }
}

/** A screen as the app reaches it, by the destination that opens it. */
private class Screen(@StringRes val name: Int, @StringRes val said: Int, val key: NavKey)

private val SCREENS = listOf(
    Screen(R.string.debug_welcome, R.string.debug_welcome_said, Welcome),
    Screen(R.string.debug_setup, R.string.debug_setup_said, Setup),
    Screen(R.string.debug_just_scanned, R.string.debug_just_scanned_said, Home(Stage.JustScanned)),
    Screen(R.string.debug_asking, R.string.debug_asking_said, Home(Stage.Asking)),
)
