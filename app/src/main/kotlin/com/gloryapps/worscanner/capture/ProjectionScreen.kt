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
import com.gloryapps.worscanner.scanner.Frame
import com.gloryapps.worscanner.scanner.Screen
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
    private val width: Int,
    private val height: Int,
    density: Int,
) : Screen {
    private class Held(val frame: BitmapFrame, val at: Long)

    private val handler = Handler(Looper.getMainLooper())
    private val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
    private val display: VirtualDisplay
    private val latest = MutableStateFlow<Held?>(null)

    init {
        /* Android 14 refuses a virtual display on a projection with no callback registered. */
        projection.registerCallback(object : MediaProjection.Callback() {}, handler)
        reader.setOnImageAvailableListener({ it.acquireLatestImage()?.use { image -> latest.value = Held(frameOf(image), System.nanoTime()) } }, handler)
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

    private fun frameOf(image: Image): BitmapFrame {
        val plane = image.planes[0]
        val stride = plane.rowStride / plane.pixelStride
        val wide = Bitmap.createBitmap(stride, height, Bitmap.Config.ARGB_8888)
        wide.copyPixelsFromBuffer(plane.buffer)

        return BitmapFrame(if (stride == width) wide else Bitmap.createBitmap(wide, 0, 0, width, height))
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
