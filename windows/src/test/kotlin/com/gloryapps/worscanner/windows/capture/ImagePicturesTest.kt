package com.gloryapps.worscanner.windows.capture

import com.gloryapps.worscanner.scanner.runs.Cut
import com.gloryapps.worscanner.scanner.runs.write
import com.gloryapps.worscanner.scanner.text.Box
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class ImagePicturesTest {
    private val folder: File = createTempDirectory().toFile()

    /** A frame painted one colour, its alpha byte left at 0 as GDI leaves it. */
    private fun frame(width: Int, height: Int, rgb: Int) = PixelFrame(width, height, IntArray(width * height) { rgb })

    @Test
    fun `a panel read under two tabs is the two cuts side by side, in the order they were read`() {
        val file = File(folder, "0.png")

        ImagePictures().write(listOf(Cut(frame(100, 50, 0xFF0000), Box(10, 5, 40, 45)), Cut(frame(100, 50, 0x0000FF), Box(60, 0, 80, 20))), file)

        val sheet = ImageIO.read(file)
        assertEquals(30 + 20, sheet.width)
        assertEquals(40, sheet.height)
        assertEquals(0xFF0000, sheet.getRGB(0, 0) and 0xFFFFFF)
        assertEquals(0x0000FF, sheet.getRGB(30, 0) and 0xFFFFFF)
    }

    @Test
    fun `a frame kept whole is the frame, opaque though GDI left its alpha at nothing`() {
        val file = File(folder, "first.png")

        ImagePictures().write(frame(64, 36, 0x123456), file)

        val image = ImageIO.read(file)
        assertEquals(64 to 36, image.width to image.height)
        assertEquals(0xFF123456.toInt(), image.getRGB(63, 35))
    }
}
