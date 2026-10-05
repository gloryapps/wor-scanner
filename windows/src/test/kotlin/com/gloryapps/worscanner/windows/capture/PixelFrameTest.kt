package com.gloryapps.worscanner.windows.capture

import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class PixelFrameTest {
    @Test
    fun `GDI's blue, green, red and spare bytes read as the colour they are`() {
        val bytes = ByteBuffer.wrap(byteArrayOf(0x30, 0x20, 0x10, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00))

        val frame = PixelFrame.ofBgra(bytes, width = 2, height = 1)

        val first = frame.colourAt(0, 0)
        assertEquals(listOf(0x10, 0x20, 0x30), listOf(first.red, first.green, first.blue))
        assertEquals(0xFF, frame.colourAt(1, 0).red)
    }

    @Test
    fun `rows run from the top`() {
        val bytes = ByteBuffer.wrap(ByteArray(2 * 2 * 4).also { it[2 * 4 + 1] = 0x7F })

        val frame = PixelFrame.ofBgra(bytes, width = 2, height = 2)

        assertEquals(0x7F, frame.colourAt(0, 1).green)
        assertEquals(0, frame.colourAt(0, 0).green)
    }
}
