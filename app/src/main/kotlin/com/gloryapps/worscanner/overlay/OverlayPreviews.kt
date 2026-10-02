package com.gloryapps.worscanner.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.scan.Progress
import com.gloryapps.worscanner.scanner.scan.ScanEntry
import com.gloryapps.worscanner.ui.ScannerTheme
import com.gloryapps.worscanner.ui.Standing

/* The strip is drawn over the game, so the previews put it on a dark ground rather than on the app's. */
private val GAME = Color(0xFF0A0C12)

private val RUNNING = ScanState.Running(Kind.GEAR, Progress(1204, 2500))
private val ENDED = ScanState.Ended(
    Kind.GEAR,
    /* The capsule counts the entries and says nothing else of them, so an entry can be an empty one. */
    Outcome.Finished(List(1204) { ScanEntry(it, 0, 0, card = Unit, rows = emptyList()) }),
    stamp = "20260907-130812",
)

@Preview(name = "Hairline · under way", widthDp = 320, heightDp = 24)
@Composable
private fun HairlinePreview() {
    ScannerTheme {
        Box(Modifier.fillMaxSize().background(GAME)) { Hairline(RUNNING) }
    }
}

@Preview(name = "Capsule · idle for each kind, under way, ended", widthDp = 320, heightDp = 300)
@Composable
private fun CapsulePreview() {
    ScannerTheme {
        Column(
            Modifier.fillMaxSize().background(GAME).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Kind.entries.forEach { Capsule(ScanState.Idle, it, { }, { }, { }, Modifier) }
            Capsule(RUNNING, Kind.GEAR, { }, { }, { }, Modifier)
            Capsule(ENDED, Kind.GEAR, { }, { }, { }, Modifier)
        }
    }
}

@Preview(name = "Capsule · the sheet open", widthDp = 320, heightDp = 340)
@Composable
private fun SheetPreview() {
    ScannerTheme {
        Column(
            Modifier.fillMaxSize().background(GAME).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Capsule(RUNNING, Kind.GEAR, { }, { }, { }, Modifier)
            Sheet(kind = Kind.GEAR, running = true, touch = Standing.ON, onChoose = { }, onScan = { }, onRead = { }, onTouch = { }, onApp = { }, onClose = { })
        }
    }
}

@Preview(name = "Sheet · each kind, nothing running", widthDp = 760, heightDp = 360)
@Composable
private fun SheetIdlePreview() {
    ScannerTheme {
        Row(Modifier.fillMaxSize().background(GAME).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Kind.entries.forEach { kind ->
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Capsule(ScanState.Idle, kind, { }, { }, { }, Modifier)
                    Sheet(kind = kind, running = false, touch = Standing.ON, onChoose = { }, onScan = { }, onRead = { }, onTouch = { }, onApp = { }, onClose = { })
                }
            }
        }
    }
}

@Preview(name = "Sheet · the accessibility service stopped, and off", widthDp = 520, heightDp = 340)
@Composable
private fun SheetUntouchedPreview() {
    ScannerTheme {
        Row(Modifier.fillMaxSize().background(GAME).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(Standing.STALLED, Standing.OFF).forEach { touch ->
                Sheet(kind = Kind.GEAR, running = false, touch = touch, onChoose = { }, onScan = { }, onRead = { }, onTouch = { }, onApp = { }, onClose = { })
            }
        }
    }
}

@Preview(name = "Close target · waiting and under the capsule", widthDp = 320, heightDp = 180)
@Composable
private fun CloseTargetPreview() {
    ScannerTheme {
        Row(
            Modifier.fillMaxSize().background(GAME).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box { CloseTarget(over = false) }
            Box { CloseTarget(over = true) }
        }
    }
}
