package com.gloryapps.worscanner.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Progress
import com.gloryapps.worscanner.ui.ScannerTheme
import com.gloryapps.worscanner.ui.TALL_H
import com.gloryapps.worscanner.ui.TALL_W
import com.gloryapps.worscanner.ui.WIDE_H
import com.gloryapps.worscanner.ui.WIDE_W

private val READY = HomeUiState(accessibilityOn = true, overlayAllowed = true)

@Preview(name = "Home · ready", widthDp = WIDE_W, heightDp = WIDE_H)
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

@Preview(name = "Home · portrait", widthDp = TALL_W, heightDp = TALL_H)
@Composable
private fun HomePortraitPreview() {
    ScannerTheme { Home(READY) { } }
}
