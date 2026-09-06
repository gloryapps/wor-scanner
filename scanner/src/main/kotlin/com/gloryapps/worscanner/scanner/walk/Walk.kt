package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.Frame
import com.gloryapps.worscanner.scanner.Screen
import com.gloryapps.worscanner.scanner.TextReader
import com.gloryapps.worscanner.scanner.Touch
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
 * too low to tap and find where the grid landed by the words on its tiles.
 *
 * It begins on the tile the game has selected, or on the first whole row in view where none is,
 * and ends where the rows run out. A piece's identity is its place in the walk, never its content.
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
     * Walks the storage from where it stands to its end, filling `entries` as it goes.
     *
     * The list is the caller's so that a cancelled walk, which ends by the exception it must, still
     * leaves the caller holding what was read before it.
     */
    suspend fun run(entries: MutableList<ScanEntry>, progress: suspend (Progress) -> Unit = {}): Outcome =
        resultOf { walk(entries, progress) }.getOrElse { Outcome.Failed(it, entries) }

    private suspend fun walk(entries: MutableList<ScanEntry>, progress: suspend (Progress) -> Unit): Outcome {
        var frame = screen.capture()
        var lines = reader.read(frame)
        val held = countIn(rowsWithin(frame, lines, layout.count))
            ?: return Outcome.Stopped(Outcome.Reason.STORAGE_NOT_OPEN, entries)
        val pitch = layout.pitchY(frame.height)
        val floor = (layout.gridBottom * frame.height).toInt()
        val grid = layout.gridBox(frame)
        val columnPitch = layout.pitchX(frame.width)
        /* A row is tapped where it sits whole; at the grid's end, where a drag moves nothing, a row still mostly in view will do. */
        val lowest = floor - pitch / 2
        val lowestAtEnd = floor - pitch / 4

        /* The centre of the row the walk began on, which the grid carries upward as it scrolls. */
        var origin = topRowCentre(lines, layout, frame) ?: return Outcome.Stopped(Outcome.Reason.STORAGE_NOT_OPEN, entries)
        val rowsInView = (0..(floor - origin) / pitch).map { origin + it * pitch }
        val (startRow, startColumn) = selectedTile(frame, layout, rowsInView) ?: (0 to 0)
        origin += startRow * pitch

        var row = 0
        var atEnd = false
        while (true) {
            while (origin + row * pitch > lowest) {
                scroll(frame)
                val moved = screen.capture()
                val movedLines = reader.read(moved)
                val shift = shiftByText(lines, movedLines, grid, columnPitch)
                    ?: return Outcome.Stopped(Outcome.Reason.GRID_LOST, entries)
                frame = moved
                lines = movedLines
                if (shift > 0) {
                    origin -= shift
                } else if (origin + row * pitch <= lowestAtEnd) {
                    atEnd = true
                    break
                } else {
                    return Outcome.Finished(entries)
                }
            }

            val centreY = origin + row * pitch
            var any = false
            for (column in (if (row == 0) startColumn else 0) until layout.columns) {
                if (!tileAt(lines, layout, frame, column, centreY)) break
                any = true
                touch.tap(layout.tileX(column, frame.width), centreY)
                delay(settleMillis)
                frame = screen.capture()
                lines = reader.read(frame)
                val panel = rowsWithin(frame, lines, layout.panel)
                var entry = ScanEntry(entries.size, row, column, readGearCard(panel), panel)
                if (entry.unread) entry = entry.copy(png = keeper.keep(frame, entry))
                entries += entry
                progress(Progress(entries.size, held))
            }
            if (!any || atEnd && !tileAt(lines, layout, frame, 0, centreY + pitch)) return Outcome.Finished(entries)
            row++
        }
    }

    /** Dragged slowly so the grid stops near where the finger does; where exactly is measured after. */
    private suspend fun scroll(frame: Frame) {
        val x = (layout.dragX * frame.width).toInt()
        val from = (layout.dragFromY * frame.height).toInt()
        touch.drag(x, from, x, from - layout.rowsPerDrag * layout.pitchY(frame.height), DRAG_MILLIS)
        delay(settleMillis)
    }

    private fun rowsWithin(frame: Frame, lines: List<Line>, region: Region): List<String> {
        val box = region.box(frame.width, frame.height)

        return rowsOf(lines.filter { box.holds(it) })
    }

    private companion object {
        const val DRAG_MILLIS = 900L
    }
}
