package com.gloryapps.worscanner.capture

import android.graphics.Bitmap
import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame

class BitmapFrame(val bitmap: Bitmap) : Frame {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height

    override fun colourAt(x: Int, y: Int) = Colour(bitmap.getPixel(x, y))
}
