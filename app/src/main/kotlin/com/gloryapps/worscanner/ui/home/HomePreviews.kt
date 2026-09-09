package com.gloryapps.worscanner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Progress
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.READ
import com.gloryapps.worscanner.ui.SCANNED
import com.gloryapps.worscanner.ui.STOPPED
import com.gloryapps.worscanner.ui.ScannerTheme
import androidx.compose.ui.unit.dp

/** The board's own frames: the game's 1280x720 inside an emulator, and a phone held upright. */
private const val WIDE_W = 1280
private const val WIDE_H = 720
private const val TALL_W = 412
private const val TALL_H = 915

private val READY = HomeUiState(
    accessibilityOn = true,
    overlayAllowed = true,
    kind = Kind.GEAR,
    readings = listOf(SCANNED, STOPPED, READ),
)

@Preview(name = "Home · before a scan", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomePreview() {
    ScannerTheme { Home(READY) { } }
}

@Preview(name = "Home · scanning", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomeScanningPreview() {
    val state = READY.copy(capturing = true, running = ScanState.Running(Kind.GEAR, Progress(1204, 2500)))
    ScannerTheme { Home(state) { } }
}

@Preview(name = "Home · nothing granted, nothing kept", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomeEmptyPreview() {
    ScannerTheme { Home(HomeUiState()) { } }
}

@Preview(name = "Home · portrait", widthDp = TALL_W, heightDp = TALL_H)
@Composable
private fun HomePortraitPreview() {
    ScannerTheme { Home(READY) { } }
}

@Preview(name = "Home · confirming a delete", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomeDeletingPreview() {
    ScannerTheme { Home(READY.copy(deleting = STOPPED)) { } }
}

@Preview(name = "Start · ready, capturing, half granted", widthDp = 560, heightDp = 900)
@Composable
private fun StartPreview() {
    ScannerTheme {
        Column(Modifier.fillMaxSize().background(Colors.screen).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Start(READY) { }
            Start(READY.copy(capturing = true)) { }
            Start(HomeUiState(accessibilityOn = true)) { }
        }
    }
}

@Preview(name = "Readings · three kept, none kept", widthDp = 560, heightDp = 520)
@Composable
private fun ReadingsPreview() {
    ScannerTheme {
        Column(Modifier.fillMaxSize().background(Colors.screen).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Readings(listOf(SCANNED, STOPPED, READ)) { }
            Readings(emptyList()) { }
        }
    }
}
