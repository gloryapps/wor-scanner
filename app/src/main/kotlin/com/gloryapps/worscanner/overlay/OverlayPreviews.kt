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
import java.io.File

/* The strip is drawn over the game, so the previews put it on a dark ground rather than on the app's. */
private val GAME = Color(0xFF0A0C12)

private val RUNNING = ScanState.Running(Kind.GEAR, Progress(1204, 2500))
private val ENDED = ScanState.Ended(
    Kind.GEAR,
    /* The capsule counts the entries and says nothing else of them, so an entry can be an empty one. */
    Outcome.Finished(List(1204) { ScanEntry(it, 0, 0, card = Unit, rows = emptyList()) }),
    File("scan.json"),
)

@Preview(name = "Capsule · every state", widthDp = 320, heightDp = 150)
@Composable
private fun CapsulePreview() {
    ScannerTheme {
        Column(
            Modifier.fillMaxSize().background(GAME).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Capsule(ScanState.Idle, { }, { }, { }, Modifier)
            Capsule(RUNNING, { }, { }, { }, Modifier)
            Capsule(ENDED, { }, { }, { }, Modifier)
        }
    }
}

@Preview(name = "Capsule · the sheet open", widthDp = 320, heightDp = 260)
@Composable
private fun SheetPreview() {
    ScannerTheme {
        Column(
            Modifier.fillMaxSize().background(GAME).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Capsule(RUNNING, { }, { }, { }, Modifier)
            Sheet(running = true, onScan = { }, onRead = { }, onApp = { }, onClose = { })
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
