package com.gloryapps.worscanner.windows.capture

import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A frame as GDI hands it back: one `0xAARRGGBB` per pixel, row after row from the top; the alpha is whatever GDI left. */
internal class PixelFrame(override val width: Int, override val height: Int, val pixels: IntArray) : Frame {
    override fun colourAt(x: Int, y: Int) = Colour(pixels[y * width + x])

    companion object {
        /** GDI's 32-bit DIB: blue, green, red and a spare byte per pixel, which read little-endian is `0xAARRGGBB`. */
        fun ofBgra(bytes: ByteBuffer, width: Int, height: Int): PixelFrame =
            PixelFrame(width, height, IntArray(width * height).also { bytes.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(it) })
    }
}
