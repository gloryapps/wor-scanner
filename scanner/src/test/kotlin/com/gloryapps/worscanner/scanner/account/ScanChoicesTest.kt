package com.gloryapps.worscanner.scanner.account

import kotlin.test.Test
import kotlin.test.assertEquals

class ScanChoicesTest {
    @Test
    fun `a scan sends +16 gear and the artifacts worth wearing until the player chooses`() {
        assertEquals(ScanChoices(setOf(Enhancement.AT_16), ArtifactsToScan.WORTH_WEARING), ScanChoices())
    }

    @Test
    fun `a band toggles in and out, and the last one stays`() {
        val more = ScanChoices().toggled(Enhancement.FROM_12)

        assertEquals(setOf(Enhancement.FROM_12, Enhancement.AT_16), more.enhancements)
        assertEquals(setOf(Enhancement.AT_16), more.toggled(Enhancement.FROM_12).enhancements)
        assertEquals(setOf(Enhancement.AT_16), ScanChoices().toggled(Enhancement.AT_16).enhancements)
    }
}
