package com.gloryapps.worscanner.scan

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class ScanFileTest {
    @Test
    fun `the file names its version, which is what the lab checks first`() {
        val scan = ScanFile<Int>(kind = "gear", startedAt = "20260909-120000", width = 1280, height = 720, outcome = "finished", entries = emptyList())

        val text = Json.encodeToString(ScanFile.serializer(Int.serializer()), scan)

        assertTrue("\"version\":2" in text, text)
    }
}
