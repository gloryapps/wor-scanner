package com.gloryapps.worscanner.capture

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.gloryapps.worscanner.scanner.Frame
import com.gloryapps.worscanner.scanner.TextReader
import com.gloryapps.worscanner.scanner.reading.Box
import com.gloryapps.worscanner.scanner.reading.Line
import kotlinx.coroutines.tasks.await

/** ML Kit's Latin model, bundled in the APK: no Play services on the device, and none on an emulator. */
class MlKitTextReader : TextReader {
    private val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun read(frame: Frame): List<Line> {
        val bitmap = (frame as BitmapFrame).bitmap
        val text = recogniser.process(InputImage.fromBitmap(bitmap, 0)).await()

        return text.textBlocks.flatMap { block -> block.lines }.mapNotNull { line ->
            line.boundingBox?.let { Line(line.text, Box(it.left, it.top, it.right, it.bottom)) }
        }
    }
}
