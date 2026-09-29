package com.gloryapps.worscanner.capture

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.senses.TextReader
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import kotlinx.coroutines.tasks.await

/** ML Kit's Latin model, bundled in the APK: a sideloaded app gets no install-time download from Play services. */
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
