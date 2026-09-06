package com.gloryapps.worscanner.capture

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import com.gloryapps.worscanner.scanner.Frame
import com.gloryapps.worscanner.scanner.Screen
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.graphics.createBitmap

/** A display mirrored into an image reader; each capture is the next frame the mirror delivers. */
class ProjectionScreen(
    private val projection: MediaProjection,
    private val width: Int,
    private val height: Int,
    density: Int,
) : Screen {
    private val handler = Handler(Looper.getMainLooper())
    private val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
    private val display: VirtualDisplay

    init {
        /* Android 14 refuses a virtual display on a projection with no callback registered. */
        projection.registerCallback(object : MediaProjection.Callback() {}, handler)
        display = checkNotNull(projection.createVirtualDisplay(
            "wor-scanner",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler,
        ))
    }

    override suspend fun capture(): Frame = withTimeout(FRAME_TIMEOUT_MS.milliseconds) {
        suspendCancellableCoroutine { continuation ->
            reader.setOnImageAvailableListener({ latest ->
                latest.setOnImageAvailableListener(null, null)
                latest.acquireLatestImage()?.use { image ->
                    val plane = image.planes[0]
                    val stride = plane.rowStride / plane.pixelStride
                    val wide = createBitmap(stride, height)
                    wide.copyPixelsFromBuffer(plane.buffer)
                    val frame = if (stride == width) wide else Bitmap.createBitmap(wide, 0, 0, width, height)
                    if (continuation.isActive) continuation.resume(BitmapFrame(frame))
                }
            }, handler)
            /* A frame already sitting in the reader would never trigger the listener. */
            reader.acquireLatestImage()?.close()
            continuation.invokeOnCancellation { reader.setOnImageAvailableListener(null, null) }
        }
    }

    fun close() {
        display.release()
        reader.close()
        projection.stop()
    }

    private companion object {
        const val FRAME_TIMEOUT_MS = 2_000L
    }
}
