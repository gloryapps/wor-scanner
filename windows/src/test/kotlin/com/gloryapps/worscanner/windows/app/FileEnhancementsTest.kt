package com.gloryapps.worscanner.windows.app

import com.gloryapps.worscanner.scanner.account.Enhancement
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class FileEnhancementsTest {
    private val file = File(createTempDirectory().toFile(), "gear-bands")

    @Test
    fun `only +16 gear is scanned until the player chooses`() {
        assertEquals(setOf(Enhancement.AT_16), FileEnhancements(file).chosen.value)
    }

    @Test
    fun `a band put in stays in the next time the app starts`() = runBlocking {
        FileEnhancements(file).toggle(Enhancement.FROM_12)

        assertEquals(setOf(Enhancement.FROM_12, Enhancement.AT_16), FileEnhancements(file).chosen.value)
    }

    @Test
    fun `the last band chosen cannot be taken out`() = runBlocking {
        val bands = FileEnhancements(file)

        bands.toggle(Enhancement.AT_16)

        assertEquals(setOf(Enhancement.AT_16), bands.chosen.value)
    }
}
