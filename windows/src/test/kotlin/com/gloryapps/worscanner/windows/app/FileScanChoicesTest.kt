package com.gloryapps.worscanner.windows.app

import com.gloryapps.worscanner.scanner.account.ArtifactsToScan
import com.gloryapps.worscanner.scanner.account.Enhancement
import com.gloryapps.worscanner.scanner.account.ScanChoices
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class FileScanChoicesTest {
    private val file = File(createTempDirectory().toFile(), "scan-choices.json")

    @Test
    fun `the scan's defaults stand until the player chooses`() {
        assertEquals(ScanChoices(), FileScanChoices(file).chosen.value)
    }

    @Test
    fun `what the player chose stands the next time the app starts`() = runBlocking {
        val chosen = ScanChoices(setOf(Enhancement.FROM_12, Enhancement.AT_16), ArtifactsToScan.EVERY)

        FileScanChoices(file).keep(chosen)

        assertEquals(chosen, FileScanChoices(file).chosen.value)
    }

    @Test
    fun `a file that will not read is the scan's defaults`() {
        file.writeText("+16 and every artifact")

        assertEquals(ScanChoices(), FileScanChoices(file).chosen.value)
    }
}
