package com.gloryapps.worscanner.ui.earlier

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gloryapps.worscanner.ui.READ
import com.gloryapps.worscanner.ui.SCANNED
import com.gloryapps.worscanner.ui.STOPPED
import com.gloryapps.worscanner.ui.ScannerTheme
import com.gloryapps.worscanner.ui.TALL_H
import com.gloryapps.worscanner.ui.TALL_W
import com.gloryapps.worscanner.ui.WIDE_H
import com.gloryapps.worscanner.ui.WIDE_W

private val KEPT = EarlierUiState(readings = listOf(SCANNED, STOPPED, READ), linked = true)

@Preview(name = "All scans", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun EarlierPreview() {
    ScannerTheme { Earlier(KEPT) { } }
}

@Preview(name = "All scans · none kept, not linked", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun EarlierEmptyPreview() {
    ScannerTheme { Earlier(EarlierUiState(readings = emptyList())) { } }
}

@Preview(name = "All scans · portrait", widthDp = TALL_W, heightDp = TALL_H)
@Composable
private fun EarlierPortraitPreview() {
    ScannerTheme { Earlier(KEPT) { } }
}

@Preview(name = "All scans · confirming a delete", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun EarlierDeletingPreview() {
    ScannerTheme { Earlier(KEPT.copy(deleting = listOf(STOPPED))) { } }
}

@Preview(name = "All scans · confirming every delete", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun EarlierDeletingAllPreview() {
    ScannerTheme { Earlier(KEPT.copy(deleting = KEPT.readings.orEmpty())) { } }
}
