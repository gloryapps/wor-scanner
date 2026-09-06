package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.Frame
import com.gloryapps.worscanner.scanner.Screen
import com.gloryapps.worscanner.scanner.TextReader
import com.gloryapps.worscanner.scanner.Touch
import com.gloryapps.worscanner.scanner.reading.Box
import com.gloryapps.worscanner.scanner.reading.Line
import com.gloryapps.worscanner.scanner.reading.readGearCard
import com.gloryapps.worscanner.scanner.reading.rowsOf
import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.delay

/** Where a panel the reader did not close goes, so the image can answer what the text could not. */
fun interface Keeper {
    suspend fun keep(frame: Frame, entry: ScanEntry): String
}

/**
 * The walk over the storage: tap a tile, read the panel, next tile; drag when the next row sits
 * too low to tap and find where the grid landed by the tiles already seen.
 *
 * A piece's identity is its place in the grid, never its content: two equal pieces are two entries.
 */
class Walk(
    private val screen: Screen,
    private val touch: Touch,
    private val reader: TextReader,
    private val keeper: Keeper,
    private val layout: StorageLayout = StorageLayout(),
    private val settleMillis: Long = 250,
) {
    /**
     * Walks the whole storage, filling `entries` as it goes.
     *
     * The list is the caller's so that a cancelled walk, which ends by the exception it must, still
     * leaves the caller holding what was read before it.
     */
    suspend fun run(entries: MutableList<ScanEntry>, progress: suspend (Progress) -> Unit = {}): Outcome =
        resultOf { walk(entries, progress) }.getOrElse { Outcome.Failed(it, entries) }

    private suspend fun walk(entries: MutableList<ScanEntry>, progress: suspend (Progress) -> Unit): Outcome {
        var frame = screen.capture()
        var lines = reader.read(frame)
        val total = countIn(rowsWithin(frame, lines, layout.count))
            ?: return Outcome.Stopped(Outcome.Reason.STORAGE_NOT_OPEN, entries)
        val rows = (total + layout.columns - 1) / layout.columns
        val pitch = layout.pitchY(frame.height)
        val floor = (layout.gridBottom * frame.height).toInt()
        val grid = layout.grid.box(frame.width, frame.height)
        val columnPitch = (layout.tilePitchX * frame.width).toInt()
        /* A row is tapped where it sits whole; at the grid's end, where a drag moves nothing, a row still mostly in view will do. */
        val lowest = floor - pitch / 2
        val lowestAtEnd = floor - pitch / 4

        /* The grid may have been left anywhere; the walk starts from row zero. */
        frame = toTop(frame, lines, grid, columnPitch).also { lines = reader.read(it) }
        /* The centre of row zero, which the grid carries upward as it scrolls. */
        var origin = (layout.firstTileY * frame.height).toInt()

        for (row in 0 until rows) {
            while (origin + row * pitch > lowest) {
                scroll(frame, layout.rowsPerDrag)
                val moved = screen.capture()
                val movedLines = reader.read(moved)
                val shift = shiftByText(lines, movedLines, grid, columnPitch)
                    ?: return Outcome.Stopped(Outcome.Reason.GRID_LOST, entries)
                frame = moved
                lines = movedLines
                if (shift > 0) {
                    origin -= shift
                } else if (origin + row * pitch <= lowestAtEnd) {
                    break
                } else {
                    return Outcome.Stopped(Outcome.Reason.GRID_LOST, entries)
                }
            }

            for (column in 0 until layout.columns) {
                val index = row * layout.columns + column
                if (index >= total) break

                touch.tap(layout.tileX(column, frame.width), origin + row * pitch)
                delay(settleMillis)
                frame = screen.capture()
                lines = reader.read(frame)
                val panel = rowsWithin(frame, lines, layout.panel)
                var entry = ScanEntry(index, row, column, readGearCard(panel), panel)
                if (entry.unread) entry = entry.copy(png = keeper.keep(frame, entry))
                entries += entry
                progress(Progress(entries.size, total))
            }
        }

        return Outcome.Finished(entries)
    }

    /** Dragged slowly so the grid stops near where the finger does; where exactly is measured after. */
    private suspend fun scroll(frame: Frame, rows: Int) {
        val x = (layout.dragX * frame.width).toInt()
        val from = (layout.dragFromY * frame.height).toInt()
        touch.drag(x, from, x, from - rows * layout.pitchY(frame.height), DRAG_MILLIS)
        delay(settleMillis)
    }

    /** Dragged downward, a screen at a time, until the grid stops moving: that is its top. */
    private suspend fun toTop(start: Frame, startLines: List<Line>, grid: Box, columnPitch: Int): Frame {
        var frame = start
        var lines = startLines
        repeat(MOST_DRAGS_TO_TOP) {
            scroll(frame, -ROWS_PER_DRAG_TO_TOP)
            val moved = screen.capture()
            val movedLines = reader.read(moved)
            val shift = shiftByText(lines, movedLines, grid, columnPitch)
            frame = moved
            lines = movedLines
            if (shift == 0) return frame
        }

        return frame
    }

    private fun rowsWithin(frame: Frame, lines: List<Line>, region: Region): List<String> {
        val box = region.box(frame.width, frame.height)

        return rowsOf(lines.filter { box.holds(it) })
    }

    private companion object {
        const val DRAG_MILLIS = 900L
        const val ROWS_PER_DRAG_TO_TOP = 3
        const val MOST_DRAGS_TO_TOP = 400
    }
}
