package com.gloryapps.worscanner.capture

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Where a frame goes when the user asks to keep one: the app's own external folder, one PNG each. */
class Captures(private val context: Context) {
    fun keep(frame: BitmapFrame): File {
        val folder = File(context.getExternalFilesDir(null), "captures").apply { mkdirs() }
        val file = File(folder, "${LocalDateTime.now().format(STAMP)}.png")
        file.outputStream().use { frame.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        return file
    }

    private companion object {
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
