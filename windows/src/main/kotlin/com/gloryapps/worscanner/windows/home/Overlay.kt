package com.gloryapps.worscanner.windows.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.ui.Accented
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.Lettering
import com.gloryapps.worscanner.ui.Reach
import com.gloryapps.worscanner.ui.resources.stop
import com.gloryapps.worscanner.windows.game.Desktop
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.esc_to_stop
import com.gloryapps.worscanner.windows.resources.paused
import org.jetbrains.compose.resources.stringResource
import java.awt.GraphicsEnvironment
import com.gloryapps.worscanner.ui.resources.Res as Shared

/**
 * The sign over the game while a scan runs, centred at the top of where it draws, where no kind taps:
 * what the scan says of itself, whether it waits for the game, and its Stop. No capture holds it.
 */
@Composable
internal fun Overlay(over: Box, progress: String?, paused: Boolean, onStop: () -> Unit) {
    /* The desktop counts the game in pixels, a window is placed in dp: the screen's scale is between them. */
    val scale = remember { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX.toFloat() }
    val x = (over.left / scale).dp + ((over.width / scale).dp - WIDTH) / 2
    val y = (over.top / scale).dp + 12.dp

    Window(
        onCloseRequest = {},
        state = rememberWindowState(position = WindowPosition(x, y), size = DpSize(WIDTH, HEIGHT)),
        title = "",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = false,
        alwaysOnTop = true,
    ) {
        LaunchedEffect(Unit) { Desktop.overGame(window.windowHandle) }
        Sign(progress, paused, onStop)
    }
}

@Composable
private fun Sign(progress: String?, paused: Boolean, onStop: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)

    Row(
        Modifier.fillMaxSize().clip(shape).background(Colors.glass).border(1.dp, Colors.glassEdge, shape).padding(start = 20.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(progress.orEmpty(), style = Lettering.label, color = Colors.text, maxLines = 1)
            Text(stringResource(if (paused) Res.string.paused else Res.string.esc_to_stop).uppercase(), style = Lettering.mark, color = if (paused) Colors.warning else Colors.muted)
        }
        Accented(stringResource(Shared.string.stop), reach = Reach.SMALL, onClick = onStop)
    }
}

private val WIDTH = 440.dp
private val HEIGHT = 56.dp
