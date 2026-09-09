package com.gloryapps.worscanner.capture

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.senses.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A display mirrored into an image reader, which keeps the newest frame it delivered.
 *
 * A capture waits a moment for a frame newer than the call, since after a tap the frame wanted is
 * the one after it; a display that has not changed delivers none, and then the newest one stands.
 */
class ProjectionScreen(
    private val projection: MediaProjection,
    private var width: Int,
    private var height: Int,
    density: Int,
) : Screen {
    private class Held(val frame: BitmapFrame, val at: Long)

    private val handler = Handler(Looper.getMainLooper())
    private var reader: ImageReader = newReader()
    private val display: VirtualDisplay
    private val latest = MutableStateFlow<Held?>(null)

    init {
        /* Android 14 refuses a virtual display on a projection with no callback registered. */
        projection.registerCallback(object : MediaProjection.Callback() {}, handler)
        display = checkNotNull(
            projection.createVirtualDisplay(
                "wor-scanner",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler,
            ),
        )
    }

    override suspend fun capture(): Frame {
        val since = System.nanoTime()
        val fresh = withTimeoutOrNull(FRESH_WAIT_MS) { latest.filter { it != null && it.at > since }.first() }

        return (fresh ?: withTimeoutOrNull(FIRST_WAIT_MS) { latest.filter { it != null }.first() })?.frame
            ?: error("the display delivered no frame")
    }

    /** The display turned or changed size: the mirror takes the new shape and forgets the frames of the old one. */
    fun resize(width: Int, height: Int, density: Int) {
        if (width == this.width && height == this.height) return
        val outgrown = reader
        this.width = width
        this.height = height
        reader = newReader()
        display.resize(width, height, density)
        display.surface = reader.surface
        latest.value = null
        outgrown.close()
    }

    /* A frame from a reader since replaced is of the old shape, and is dropped. */
    private fun newReader(): ImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).apply {
        setOnImageAvailableListener({ if (it === reader) it.acquireLatestImage()?.use { image -> latest.value = Held(frameOf(image), System.nanoTime()) } }, handler)
    }

    private fun frameOf(image: Image): BitmapFrame {
        val plane = image.planes[0]
        val stride = plane.rowStride / plane.pixelStride
        val wide = Bitmap.createBitmap(stride, image.height, Bitmap.Config.ARGB_8888)
        wide.copyPixelsFromBuffer(plane.buffer)

        return BitmapFrame(if (stride == image.width) wide else Bitmap.createBitmap(wide, 0, 0, image.width, image.height))
    }

    fun close() {
        display.release()
        reader.close()
        projection.stop()
    }

    private companion object {
        const val FRESH_WAIT_MS = 400L
        const val FIRST_WAIT_MS = 3_000L
    }
}
