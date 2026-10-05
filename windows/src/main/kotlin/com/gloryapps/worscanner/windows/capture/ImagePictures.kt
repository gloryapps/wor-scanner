package com.gloryapps.worscanner.windows.capture

import com.gloryapps.worscanner.scanner.runs.Cut
import com.gloryapps.worscanner.scanner.runs.Pictures
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** Frames the desktop gave, cut and laid side by side on an image of their own; GDI's spare byte is no alpha, so the image has none. */
internal class ImagePictures : Pictures {
    override fun write(cuts: List<Cut>, file: File) {
        val sheet = BufferedImage(cuts.sumOf { it.box.width }, cuts.maxOf { it.box.height }, BufferedImage.TYPE_INT_RGB)
        cuts.fold(0) { left, cut ->
            val frame = cut.frame as PixelFrame
            val box = cut.box
            sheet.setRGB(left, 0, box.width, box.height, frame.pixels, box.top * frame.width + box.left, frame.width)
            left + box.width
        }
        check(ImageIO.write(sheet, "png", file)) { "no PNG writer" }
    }
}
