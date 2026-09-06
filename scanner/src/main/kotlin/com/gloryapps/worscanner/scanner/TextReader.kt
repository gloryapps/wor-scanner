package com.gloryapps.worscanner.scanner

import com.gloryapps.worscanner.scanner.reading.Line

/** The recogniser: every line of text a frame holds, each with the box it sat in. */
interface TextReader {
    suspend fun read(frame: Frame): List<Line>
}
