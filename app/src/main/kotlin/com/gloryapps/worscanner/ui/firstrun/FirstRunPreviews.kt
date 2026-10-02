package com.gloryapps.worscanner.ui.firstrun

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gloryapps.worscanner.ui.Grants
import com.gloryapps.worscanner.ui.Permission
import com.gloryapps.worscanner.ui.ScannerTheme
import com.gloryapps.worscanner.ui.TALL_H
import com.gloryapps.worscanner.ui.TALL_W
import com.gloryapps.worscanner.ui.WIDE_H
import com.gloryapps.worscanner.ui.WIDE_W

@Preview(name = "First run · what it does", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun IntroPreview() {
    ScannerTheme { Intro { } }
}

@Preview(name = "First run · none on", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun GrantsNonePreview() {
    ScannerTheme { Grants(GrantsUiState()) { } }
}

@Preview(name = "First run · one on", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun GrantsOnePreview() {
    ScannerTheme { Grants(GrantsUiState(Grants.on(Permission.OVERLAY))) { } }
}

@Preview(name = "First run · all on", widthDp = WIDE_W, heightDp = WIDE_H)
@Composable
private fun GrantsAllPreview() {
    ScannerTheme { Grants(GrantsUiState(Grants.ALL_ON)) { } }
}

@Preview(name = "First run · portrait", widthDp = TALL_W, heightDp = TALL_H)
@Composable
private fun IntroPortraitPreview() {
    ScannerTheme { Intro { } }
}
