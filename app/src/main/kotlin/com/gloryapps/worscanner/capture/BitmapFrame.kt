package com.gloryapps.worscanner.capture

import android.graphics.Bitmap
import com.gloryapps.worscanner.scanner.Frame

class BitmapFrame(val bitmap: Bitmap) : Frame {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height
}
