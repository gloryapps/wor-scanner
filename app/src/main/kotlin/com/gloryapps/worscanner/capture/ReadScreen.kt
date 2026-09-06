package com.gloryapps.worscanner.capture

import com.gloryapps.worscanner.scanner.TextReader
import com.gloryapps.worscanner.scanner.reading.readGearCard
import com.gloryapps.worscanner.scanner.resultOf

/** The frame on screen right now, read whole and kept on disk with what the reader made of it. */
class ReadScreen(
    private val session: CaptureSession,
    private val reader: TextReader,
    private val readings: Readings,
) {
    class Read(val kept: Kept, val lines: Int)

    suspend fun now(): Result<Read> = resultOf {
        val screen = checkNotNull(session.screen.value) { "no capture session" }
        val frame = screen.capture() as BitmapFrame
        val lines = reader.read(frame)
        val reading = Reading(frame.width, frame.height, lines, readGearCard(lines))

        Read(readings.keep(frame, reading), lines.size)
    }
}
