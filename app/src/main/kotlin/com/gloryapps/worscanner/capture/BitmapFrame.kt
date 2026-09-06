package com.gloryapps.worscanner.capture

import android.graphics.Bitmap
import android.graphics.Color
import com.gloryapps.worscanner.scanner.Frame

class BitmapFrame(val bitmap: Bitmap) : Frame {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height

    override fun luminanceAt(x: Int, y: Int): Int {
        val pixel = bitmap.getPixel(x, y)

        return (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
    }
}
