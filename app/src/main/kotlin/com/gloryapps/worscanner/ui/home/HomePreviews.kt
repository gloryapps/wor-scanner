package com.gloryapps.worscanner.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gloryapps.worscanner.azhor.Linking
import com.gloryapps.worscanner.azhor.Sending
import com.gloryapps.worscanner.capture.Emulator
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.SharedFolder
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Progress
import com.gloryapps.worscanner.ui.Permission
import com.gloryapps.worscanner.ui.SCANNED
import com.gloryapps.worscanner.ui.STOPPED
import com.gloryapps.worscanner.ui.ScannerTheme
import com.gloryapps.worscanner.ui.TALL_H
import com.gloryapps.worscanner.ui.TALL_W
import com.gloryapps.worscanner.ui.WIDE_H
import com.gloryapps.worscanner.ui.WIDE_W
import com.gloryapps.worscanner.update.Release
import com.gloryapps.worscanner.update.Update
import com.gloryapps.worscanner.update.Version

private val READY = HomeUiState(granted = Permission.entries.toSet())

private val RELEASE = Release(Version(listOf(1, 5, 0)), apk = "", page = "")

@Preview(name = "Home · ready", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomePreview() {
    ScannerTheme { Home(READY) { } }
}

@Preview(name = "Home · update available", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomeUpdatePreview() {
    ScannerTheme { Home(READY.copy(update = Update.Available(RELEASE))) { } }
}

@Preview(name = "Home · update downloading", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomeUpdateDownloadingPreview() {
    ScannerTheme { Home(READY.copy(update = Update.Downloading(RELEASE))) { } }
}

@Preview(name = "Home · update failed", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomeUpdateFailedPreview() {
    ScannerTheme { Home(READY.copy(update = Update.Failed(RELEASE))) { } }
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

@Preview(name = "Home · asking, accessibility and notifications off", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomeAskingPreview() {
    val state = HomeUiState(granted = setOf(Permission.OVERLAY))
    ScannerTheme {
        Home(state) { }
        Ask(state.missing, state.ready, { }, opened = setOf(Permission.ACCESSIBILITY))
    }
}

@Preview(name = "Home · asking, all on since", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun HomeAskedPreview() {
    ScannerTheme {
        Home(READY) { }
        Ask(READY.missing, READY.ready, { })
    }
}

private val LD_PLAYER = SharedFolder(Emulator.LD_PLAYER, "/mnt/shared/Pictures")

private fun scanned(send: Send, scan: Kept.Scan = SCANNED) =
    READY.copy(justScanned = JustScanned(scan, Kind.GEAR, touched = true, shared = LD_PLAYER, send = send))

@Preview(name = "Just scanned · linked", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedPreview() {
    ScannerTheme { Home(scanned(Send.Idle)) { } }
}

@Preview(name = "Just scanned · stopped", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedStoppedPreview() {
    ScannerTheme { Home(scanned(Send.Idle, STOPPED.copy(entries = 412, detail = "stopped by the user"))) { } }
}

@Preview(name = "Just scanned · stopped, not linked", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedStoppedLinkingPreview() {
    ScannerTheme { Home(scanned(Send.Code(failed = Linking.Refused), STOPPED.copy(entries = 412, detail = "stopped by the user"))) { } }
}

@Preview(name = "Just scanned · not linked", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedLinkingPreview() {
    ScannerTheme { Home(scanned(Send.Code())) { } }
}

@Preview(name = "Just scanned · code refused", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedRefusedCodePreview() {
    ScannerTheme { Home(scanned(Send.Code(failed = Linking.Refused))) { } }
}

@Preview(name = "Just scanned · sending", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedSendingPreview() {
    ScannerTheme { Home(scanned(Send.Underway)) { } }
}

@Preview(name = "Just scanned · unlinked", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedUnlinkedPreview() {
    ScannerTheme { Home(scanned(Send.Unsent(Sending.Unlinked))) { } }
}

@Preview(name = "Just scanned · too large", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedTooLargePreview() {
    ScannerTheme { Home(scanned(Send.Unsent(Sending.TooLarge))) { } }
}

@Preview(name = "Just scanned · unreachable", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedUnreachablePreview() {
    ScannerTheme { Home(scanned(Send.Unsent(Sending.Unanswered))) { } }
}

@Preview(name = "Just scanned · sent", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ScannedSentPreview() {
    ScannerTheme { Home(scanned(Send.Sent)) { } }
}

@Preview(name = "Just scanned · portrait, no emulator folder", widthDp = TALL_W, heightDp = TALL_H)
@Composable
private fun ScannedPortraitPreview() {
    ScannerTheme { Home(READY.copy(justScanned = JustScanned(SCANNED, Kind.GEAR, touched = false, shared = null, send = Send.Code()))) { } }
}
