package com.gloryapps.worscanner.capture

import android.graphics.Bitmap
import android.graphics.Canvas
import com.gloryapps.worscanner.scanner.runs.Cut
import com.gloryapps.worscanner.scanner.runs.Pictures
import java.io.File

/** Frames the projection captured, cut and laid side by side on a bitmap of their own. */
class BitmapPictures : Pictures {
    override fun write(cuts: List<Cut>, file: File) {
        val pieces = cuts.map { cut ->
            val box = cut.box
            val bitmap = (cut.frame as BitmapFrame).bitmap
            /* A frame kept whole is written as it is, not copied first: a 720p frame is 3.7 MB. */
            if (box.width == bitmap.width && box.height == bitmap.height) bitmap else Bitmap.createBitmap(bitmap, box.left, box.top, box.width, box.height)
        }
        val sheet = pieces.singleOrNull() ?: Bitmap.createBitmap(pieces.sumOf { it.width }, pieces.maxOf { it.height }, Bitmap.Config.ARGB_8888).also { sheet ->
            Canvas(sheet).let { canvas -> pieces.fold(0f) { left, piece -> canvas.drawBitmap(piece, left, 0f, null); left + piece.width } }
        }
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
