package com.gloryapps.worscanner.scanner.senses

import com.gloryapps.worscanner.scanner.text.Line

/** The recogniser: every line of text a frame holds, each with the box it sat in. */
interface TextReader {
    suspend fun read(frame: Frame): List<Line>
}
