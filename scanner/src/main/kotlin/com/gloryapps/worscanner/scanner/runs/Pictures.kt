package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Box
import java.io.File

/** How the platform writes frames into a PNG: each cut to its box, laid side by side in order. */
interface Pictures {
    fun write(cuts: List<Cut>, file: File)
}

/** A frame and the part of it kept. */
class Cut(val frame: Frame, val box: Box)

/** The frame whole, in a PNG of its own. */
fun Pictures.write(frame: Frame, file: File) = write(listOf(Cut(frame, Box(0, 0, frame.width, frame.height))), file)
