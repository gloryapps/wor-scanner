package com.gloryapps.worscanner.windows

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gloryapps.worscanner.ui.Built
import com.gloryapps.worscanner.ui.ScannerTheme
import com.gloryapps.worscanner.windows.app.windowsModule
import com.gloryapps.worscanner.windows.home.HomeScreen
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = Built.NAME, state = rememberWindowState(width = 420.dp, height = 640.dp)) {
        KoinApplication(configuration = koinConfiguration { modules(windowsModule) }) {
            ScannerTheme {
                HomeScreen(onFront = window::toFront)
            }
        }
    }
}
