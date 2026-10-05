package com.gloryapps.worscanner.ui

import com.gloryapps.worscanner.scanner.runs.Kept
import com.gloryapps.worscanner.scanner.kinds.Kind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class KeepsTest {
    private val stamp = "20260907-130812"

    @Test
    fun `a scan goes out as the lab's file, its kept panels beside it`() {
        val kept = Kept.Scan(stamp, listOf(file("scan.json"), file("3.png"), file("first.png")), Kind.GEAR)

        assertEquals(
            listOf("wor-gear-$stamp.json", "wor-gear-$stamp-3.png", "wor-gear-$stamp-first.png"),
            kept.outbound().map { it.name },
        )
    }

    @Test
    fun `a reading goes out as one file and the frame it was read from`() {
        val kept = Kept.Read(stamp, listOf(file("$stamp.json"), file("$stamp.png")), Kind.GEAR)

        assertEquals(listOf("wor-gear-$stamp.json", "wor-gear-$stamp.png"), kept.outbound().map { it.name })
    }

    @Test
    fun `two scans of the same kind never land on each other`() {
        val first = Kept.Scan(stamp, listOf(file("scan.json"), file("0.png")), Kind.GEAR)
        val second = Kept.Scan("20260907-141500", listOf(file("scan.json"), file("0.png")), Kind.GEAR)

        assertEquals(emptySet(), first.outbound().map { it.name }.toSet() intersect second.outbound().map { it.name }.toSet())
    }

    @Test
    fun `a scan whose JSON names no kind this app knows still leaves under its stamp`() {
        val kept = Kept.Scan(stamp, listOf(file("scan.json")), kind = null)

        assertEquals(listOf("wor-$stamp.json"), kept.outbound().map { it.name })
    }

    private fun file(name: String) = File("/scans/$stamp/$name")
}
