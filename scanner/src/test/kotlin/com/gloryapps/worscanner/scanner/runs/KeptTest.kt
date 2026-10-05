package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.kinds.Kind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class KeptTest {
    private val stamp = "20260907-130812"

    private fun file(name: String) = File("/scans/$stamp/$name")

    @Test
    fun `only a scan's JSON is sent to the lab, never its panels or a reading's file`() {
        val scan = Kept.Scan(stamp, listOf(file("scan.json"), file("3.png")), Kind.GEAR)
        val read = Kept.Read(stamp, listOf(file("$stamp.json"), file("$stamp.png")), Kind.GEAR)

        assertEquals(listOf(file("scan.json")), scan.scans())
        assertEquals(emptyList(), read.scans())
    }
}
