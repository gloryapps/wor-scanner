package com.gloryapps.worscanner.windows.ocr

import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.senses.TextReader
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import com.gloryapps.worscanner.windows.capture.PixelFrame
import com.sun.jna.Memory
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor

/** Windows' own OCR, `Windows.Media.Ocr`, in the languages of the player's Windows profile: nothing downloaded, nothing shipped. */
internal class WindowsOcr : TextReader {
    /* Made on the WinRT thread by the first frame read, and kept for the life of the app. */
    private val ocr by lazy { Ocr() }

    override suspend fun read(frame: Frame): List<Line> = withContext(WinRtThread) { ocr.read(frame as PixelFrame) }
}

/** The engine and the two factories every frame needs; slots and ids from Microsoft's generated bindings (windows-rs). */
private class Ocr {
    private val engine: Inspectable
    private val largest: Int
    private val bitmaps = factoryOf("Windows.Graphics.Imaging.SoftwareBitmap", SOFTWARE_BITMAP_FACTORY)
    private val buffers = factoryOf("Windows.Security.Cryptography.CryptographicBuffer", CRYPTOGRAPHIC_BUFFER_STATICS)

    init {
        factoryOf("Windows.Media.Ocr.OcrEngine", OCR_ENGINE_STATICS).use { statics ->
            largest = IntByReference().also { statics.call(STATICS_MAX_IMAGE_DIMENSION, it).ok("the OCR's largest image") }.value
            val made = PointerByReference()
            statics.call(STATICS_TRY_CREATE_FROM_USER_PROFILE_LANGUAGES, made).ok("an OCR engine")
            engine = Inspectable(checkNotNull(made.value) { "Windows has OCR for none of this profile's languages" })
        }
    }

    fun read(frame: PixelFrame): List<Line> {
        check(maxOf(frame.width, frame.height) <= largest) { "the game's window, ${frame.width}x${frame.height}, is larger than Windows' OCR reads, $largest" }

        return bitmapOf(frame).use { bitmap ->
            engine.get(ENGINE_RECOGNIZE_ASYNC, "recognising the frame", bitmap.pointer).use { recognising ->
                recognising.result("recognising the frame").use { result -> linesOf(wordsOf(result)) }
            }
        }
    }

    /* Every pixel made opaque: GDI's fourth byte is no alpha, and a bitmap read as transparent reads as nothing. */
    private fun bitmapOf(frame: PixelFrame): Inspectable {
        val pixels = IntArray(frame.pixels.size) { frame.pixels[it] or OPAQUE }
        val bytes = Memory(pixels.size * 4L).apply { write(0, pixels, 0, pixels.size) }
        val bitmap = bitmaps.get(FACTORY_CREATE_WITH_ALPHA, "a bitmap", BGRA8, frame.width, frame.height, ALPHA_IGNORED)
        try {
            buffers.get(STATICS_CREATE_FROM_BYTE_ARRAY, "a buffer", pixels.size * 4, bytes).use { buffer ->
                bitmap.call(BITMAP_COPY_FROM_BUFFER, buffer.pointer).ok("filling the bitmap")
            }
        } catch (failure: IllegalStateException) {
            bitmap.close()
            throw failure
        }

        return bitmap
    }

    private fun wordsOf(result: Inspectable): List<List<Word>> = result.get(RESULT_LINES, "the lines").use { lines ->
        each(lines) { line -> line.get(LINE_WORDS, "a line's words").use { words -> each(words, ::wordOf) } }
    }

    private fun <T> each(vector: Inspectable, read: (Inspectable) -> T): List<T> {
        val size = IntByReference().also { vector.call(VECTOR_SIZE, it).ok("a list's size") }.value

        return (0 until size).map { at -> vector.get(VECTOR_GET_AT, "item $at", at).use(read) }
    }

    private fun wordOf(word: Inspectable): Word {
        val rect = Memory(4L * Float.SIZE_BYTES)
        word.call(WORD_BOUNDING_RECT, rect).ok("a word's box")
        val (x, y, width, height) = rect.getFloatArray(0, 4)

        return Word(word.text(WORD_TEXT, "a word"), Box(floor(x).toInt(), floor(y).toInt(), ceil(x + width).toInt(), ceil(y + height).toInt()))
    }

    private companion object {
        const val OCR_ENGINE_STATICS = "{5BFFA85A-3384-3540-9940-699120D428A8}"
        const val STATICS_MAX_IMAGE_DIMENSION = 6
        const val STATICS_TRY_CREATE_FROM_USER_PROFILE_LANGUAGES = 10
        const val ENGINE_RECOGNIZE_ASYNC = 6
        const val RESULT_LINES = 6
        const val LINE_WORDS = 6
        const val WORD_BOUNDING_RECT = 6
        const val WORD_TEXT = 7
        const val VECTOR_GET_AT = 6
        const val VECTOR_SIZE = 7

        const val SOFTWARE_BITMAP_FACTORY = "{C99FEB69-2D62-4D47-A6B3-4FDB6A07FDF8}"
        const val FACTORY_CREATE_WITH_ALPHA = 7
        const val BITMAP_COPY_FROM_BUFFER = 17
        /** `BitmapPixelFormat.Bgra8` and `BitmapAlphaMode.Ignore`. */
        const val BGRA8 = 87
        const val ALPHA_IGNORED = 2

        const val CRYPTOGRAPHIC_BUFFER_STATICS = "{320B7E22-3CB0-4CDF-8663-1D28910065EB}"
        const val STATICS_CREATE_FROM_BYTE_ARRAY = 9

        const val OPAQUE = 0xFF000000.toInt()
    }
}
