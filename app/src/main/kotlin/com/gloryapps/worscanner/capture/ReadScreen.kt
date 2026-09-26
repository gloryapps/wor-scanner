package com.gloryapps.worscanner.capture

import com.gloryapps.worscanner.report.CrashReports
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.kinds.scan
import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.scanner.scan.Scan
import com.gloryapps.worscanner.scanner.scan.Seen
import com.gloryapps.worscanner.scanner.senses.TextReader

/** The frame on screen right now, read whole as one kind and kept on disk with what the reader made of it. */
class ReadScreen(
    private val session: CaptureSession,
    private val reader: TextReader,
    private val readings: Readings,
    private val reports: CrashReports,
) {
    class Read(val kept: Kept, val lines: Int)

    suspend fun now(kind: Kind): Result<Read> = resultOf { now(kind, kind.scan()) }.onFailure { reports.failed("read of ${kind.id}", it) }

    private suspend fun <T> now(kind: Kind, scan: Scan<T>): Read {
        val screen = checkNotNull(session.screen.value) { "no capture session" }
        val frame = screen.capture() as BitmapFrame
        val lines = reader.read(frame)
        val reading = ReadingFile(kind.id, frame.width, frame.height, lines, scan.readScreen(Seen(frame, lines)))

        return Read(readings.keep(frame, reading, scan.serializer), lines.size)
    }
}
