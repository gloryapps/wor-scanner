package com.gloryapps.worscanner.capture

import android.graphics.Bitmap
import android.graphics.Color
import com.gloryapps.worscanner.scanner.senses.Frame

class BitmapFrame(val bitmap: Bitmap) : Frame {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height

    override fun palenessAt(x: Int, y: Int): Int {
        val pixel = bitmap.getPixel(x, y)

        return minOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
    }
}
