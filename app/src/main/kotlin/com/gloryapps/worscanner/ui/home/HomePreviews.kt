package com.gloryapps.worscanner.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Progress
import com.gloryapps.worscanner.ui.READ
import com.gloryapps.worscanner.ui.SCANNED
import com.gloryapps.worscanner.ui.STOPPED
import com.gloryapps.worscanner.ui.ScannerTheme

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
