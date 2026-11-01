package com.gloryapps.worscanner.scanner.client

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnityBundleTest {
    private val skins = mufTable(listOf("skins"), mapOf(1 to numbers(0)))
    private val units = mufTable(listOf("units"), mapOf(2903 to numbers(0)))

    @Test
    fun `a bundle's table is found by its name among the others it holds`() {
        assertContentEquals(units, assetIn(bundleOf("UnitSkin" to skins, "Unit" to units), "Unit"))
    }

    @Test
    fun `a bundle holding no table by the name is none to read`() {
        assertFailsWith<IllegalArgumentException> { assetIn(bundleOf("UnitSkin" to skins), "Unit") }
    }

    @Test
    fun `an LZ4 copy may overlap what it copies`() {
        // Three literals, a copy of six from three back, then a last literal.
        val packed = byteArrayOf(0x32, 'a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte(), 3, 0, 0x10, 'x'.code.toByte())

        assertEquals("abcabcabcx", lz4(packed, size = 10).decodeToString())
    }
}
