package com.gloryapps.worscanner.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gloryapps.worscanner.ui.Built
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.ScannerTheme
import com.gloryapps.worscanner.windows.app.windowsModule
import com.gloryapps.worscanner.windows.home.HomeScreen
import com.gloryapps.worscanner.windows.resources.Res
import com.gloryapps.worscanner.windows.resources.icon
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import kotlin.math.roundToInt

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = Built.NAME, icon = painterResource(Res.drawable.icon), state = rememberWindowState(width = 420.dp, height = Dp.Unspecified)) {
        val density = LocalDensity.current
        KoinApplication(configuration = koinConfiguration { modules(windowsModule) }) {
            ScannerTheme {
                Box(Modifier.fillMaxSize().background(Colors.screen)) {
                    /* As tall as the screen holds now and the frame, set on the window itself: through WindowState, the echo of the last resize undoes it. */
                    HomeScreen(
                        Modifier.wrapContentHeight(Alignment.Top, unbounded = true).onSizeChanged { content ->
                            val frame = window.insets.let { it.top + it.bottom }
                            window.setSize(window.width, with(density) { content.height.toDp() }.value.roundToInt() + frame)
                        },
                    )
                }
            }
        }
    }
}
