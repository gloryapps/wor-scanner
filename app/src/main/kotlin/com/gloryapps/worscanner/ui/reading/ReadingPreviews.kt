package com.gloryapps.worscanner.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.ui.Colors
import com.gloryapps.worscanner.ui.SCANNED
import com.gloryapps.worscanner.ui.STOPPED
import com.gloryapps.worscanner.ui.ScannerTheme
import com.gloryapps.worscanner.ui.TALL_H
import com.gloryapps.worscanner.ui.TALL_W
import com.gloryapps.worscanner.ui.WIDE_H
import com.gloryapps.worscanner.ui.WIDE_W

private val CARD = """
    {
        "set": "cataclysm",
        "slot": "BANGLE",
        "ancient": false,
        "variant": true,
        "exclusive": "Vierna",
        "attributes": [
            { "name": "ATK", "value": 469.0, "unit": "FLAT", "bonus": null },
            { "name": "Crit. Rate", "value": 13.5, "unit": "PERCENTAGE", "bonus": null },
            { "name": "HP Bonus", "value": 17.0, "unit": "PERCENTAGE", "bonus": 2.5 }
        ]
    }
""".trimIndent()

private val PIECES = listOf(
    Piece(0, 0, 0, "Vierna's Bangle", "T3 · +16 · Cataclysm · ATK 469", CARD, closed = true),
    Piece(1, 0, 1, "Tempered Will Bangle", "T3 · +16 · Tempered Will · ATK 455", CARD, closed = true),
    Piece(2, 0, 2, "Stormcaller Blade", "T3 · +16 · Cataclysm · ATK 1056", CARD, closed = true),
    Piece(3, 0, 3, "Tidal Mist Helm", "T2 · +16 · Tempered Will · HP 4210", CARD, closed = true),
    Piece(4, 0, 4, "Ashen Ring", "T2 · +12 · the block at the foot did not read", CARD, closed = false),
    Piece(5, 0, 5, "Warden Plate", "T2 · +12 · Tempered Will · DEF 728", CARD, closed = true),
)

private val OPENED = ReadingUiState(kept = SCANNED, pieces = PIECES, file = CARD)

@Preview(name = "Reading · a piece", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ReadingPreview() {
    ScannerTheme { Reading(OPENED) { } }
}

@Preview(name = "Reading · the file as written", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ReadingFilePreview() {
    ScannerTheme { Reading(OPENED.copy(showing = Showing.FILE)) { } }
}

@Preview(name = "Reading · a scan that read nothing", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ReadingEmptyPreview() {
    val state = ReadingUiState(kept = STOPPED, file = CARD, showing = Showing.FILE)
    ScannerTheme { Reading(state) { } }
}

@Preview(name = "Reading · a piece the reader did not close", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun ReadingOpenPreview() {
    ScannerTheme { Reading(OPENED.copy(chosen = 4)) { } }
}

@Preview(name = "Reading · portrait, the list", widthDp = TALL_W, heightDp = TALL_H)
@Composable
private fun ReadingPortraitPreview() {
    ScannerTheme { Reading(OPENED) { } }
}

@Preview(name = "Reading · portrait, a piece", widthDp = TALL_W, heightDp = TALL_H)
@Composable
private fun ReadingPortraitPiecePreview() {
    ScannerTheme { Reading(OPENED.copy(chosen = 4, opened = true)) { } }
}

@Preview(name = "Pieces", widthDp = 396, heightDp = 480)
@Composable
private fun PiecesPreview() {
    ScannerTheme { Pieces(OPENED, { }, Modifier.width(LIST).fillMaxHeight().background(Colors.screen)) }
}

@Preview(name = "Detail · closed beside one the reader did not close", widthDp = 900, heightDp = 480)
@Composable
private fun DetailPreview() {
    ScannerTheme {
        Row(Modifier.fillMaxSize().background(Colors.screen)) {
            Detail(OPENED, { }, Modifier.weight(1f))
            Detail(OPENED.copy(chosen = 4), { }, Modifier.weight(1f))
        }
    }
}

@Preview(name = "Written", widthDp = 560, heightDp = 480)
@Composable
private fun WrittenPreview() {
    ScannerTheme { Written(OPENED, { }, Modifier.fillMaxSize().background(Colors.screen)) }
}
