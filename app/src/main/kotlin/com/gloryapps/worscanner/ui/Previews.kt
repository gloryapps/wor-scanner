package com.gloryapps.worscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gloryapps.worscanner.capture.Emulator
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.scan.Ended
import com.gloryapps.worscanner.capture.SharedFolder
import com.gloryapps.worscanner.scanner.kinds.Kind
import java.io.File

/**
 * What the previews are drawn from. A `Kept` names files that need not exist: only its stamp, its
 * counts and how it ended reach the screen, and a file that is not there weighs nothing.
 */
internal val SCANNED = Kept.Scan(
    stamp = "20260907-130841",
    files = listOf(File("scan.json"), File("5.png")),
    kind = Kind.GEAR,
    entries = 1204,
    ended = Ended.FINISHED,
)

internal val STOPPED = Kept.Scan(
    stamp = "20260907-130118",
    files = listOf(File("scan.json")),
    kind = Kind.GEAR,
    entries = 7,
    ended = Ended.STOPPED,
    detail = "no count like 1,169/2,500 in the header",
)

internal val READ = Kept.Read(
    stamp = "20260904-082442",
    files = listOf(File("20260904-082442.json"), File("20260904-082442.png")),
)

private val LD_PLAYER = SharedFolder(Emulator.LD_PLAYER, "/mnt/shared/Pictures")
private val BLUE_STACKS = SharedFolder(Emulator.BLUE_STACKS, "/sdcard/windows/BstSharedFolder")

@Preview(name = "Controls", widthDp = 520, heightDp = 460)
@Composable
private fun ControlsPreview() {
    ScannerTheme {
        Column(
            Modifier.fillMaxSize().background(Colors.screen).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("Controls")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Accented("Start", said = "→ game") { }
                Edged("Overlay only") { }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Accented("Start", enabled = false) { }
                Inline("grant") { }
                Inline("export", accented = true) { }
                Link("export all") { }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Gear", chosen = true) { }
                Pill("Heroes") { }
                Pill("complete")
                Destructive("Delete") { }
            }
            var showing by remember { mutableIntStateOf(0) }
            Segmented(listOf("Data", "JSON"), showing) { showing = it }
            Card(Modifier.fillMaxWidth(), leading = true) {
                Text("A card that leads", style = Lettering.title, color = Colors.text)
                Text("And what it says under its name.", style = Lettering.body, color = Colors.muted)
            }
        }
    }
}

@Preview(name = "Lettering", widthDp = 520, heightDp = 380)
@Composable
private fun LetteringPreview() {
    ScannerTheme {
        Column(
            Modifier.fillMaxSize().background(Colors.screen).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("title · Scan your gear", style = Lettering.title, color = Colors.text)
            Text("subtitle · Vierna's Bangle", style = Lettering.subtitle, color = Colors.text)
            Text("brand · WoR Scanner", style = Lettering.brand, color = Colors.text)
            Section("section · permissions")
            Text("body · what the card says under its name.", style = Lettering.body, color = Colors.muted)
            Text("caption · and what is said under that.", style = Lettering.caption, color = Colors.muted)
            Text("action · Save", style = Lettering.action, color = Colors.text)
            Text("data · 20260907-130841", style = Lettering.data, color = Colors.text)
            Text("dataSmall · 1204/2500", style = Lettering.dataSmall, color = Colors.muted)
        }
    }
}

@Preview(name = "Export sheet", widthDp = 560, heightDp = 520)
@Composable
private fun ExportSheetPreview() {
    ScannerTheme {
        ExportSheet(
            outgoing = Outgoing(
                name = "wor-gear-20260907-130841.json",
                files = SCANNED.outbound(),
                holds = listOf("Kind" to "Gear", "Read" to "1204", "Scan" to "complete", "Size" to "318 kB"),
                scans = listOf(java.io.File("scan.json")),
            ),
            failed = null,
            shared = listOf(LD_PLAYER, BLUE_STACKS),
            into = LD_PLAYER,
            onChoose = { },
            onShared = { },
            linked = true,
            sending = false,
            onShare = { },
            onSend = { },
            onClose = { },
        )
    }
}

@Preview(name = "Confirm", widthDp = 460, heightDp = 260)
@Composable
private fun ConfirmPreview() {
    ScannerTheme {
        Confirm(
            title = "Delete this reading?",
            said = "Its JSON and the panels beside it go from the device. Whatever you have already exported stays where you saved it.",
            confirm = "Delete",
            onConfirm = { },
            onCancel = { },
        )
    }
}

@Preview(name = "Export sheet · failed", widthDp = 560, heightDp = 620)
@Composable
private fun ExportSheetFailedPreview() {
    ScannerTheme {
        ExportSheet(
            outgoing = Outgoing(
                name = "wor-gear-20260907-130841.json",
                files = SCANNED.outbound(),
                holds = listOf("Kind" to "Gear", "Read" to "1204", "Size" to "318 kB"),
            ),
            failed = "/sdcard/windows/BstSharedFolder is not writable on this device",
            shared = listOf(BLUE_STACKS),
            into = BLUE_STACKS,
            onChoose = { },
            onShared = { },
            linked = false,
            sending = false,
            onShare = { },
            onSend = { },
            onClose = { },
        )
    }
}

@Preview(name = "Export sheet · no folder", widthDp = 560, heightDp = 560)
@Composable
private fun ExportSheetNoFolderPreview() {
    ScannerTheme {
        ExportSheet(
            outgoing = Outgoing(
                name = "wor-gear-20260907-130841.json",
                files = SCANNED.outbound(),
                holds = listOf("Kind" to "Gear", "Read" to "1204", "Size" to "318 kB"),
            ),
            failed = null,
            shared = emptyList(),
            into = null,
            onChoose = { },
            onShared = { },
            linked = false,
            sending = false,
            onShare = { },
            onSend = { },
            onClose = { },
        )
    }
}
