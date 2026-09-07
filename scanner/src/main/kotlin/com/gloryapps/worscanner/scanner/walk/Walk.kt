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
import kotlin.math.abs

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
            ?: return Outcome.Stopped(Outcome.Reason.STORAGE_NOT_OPEN, entries, "no count like 1,169/2,500 in the header", rowsOf(lines))
        val pitch = layout.pitchY(frame.height)
        val floor = (layout.gridBottom * frame.height).toInt()
        val grid = layout.gridBox(frame)
        val columnPitch = layout.pitchX(frame.width)
        /* A row is tapped where it sits whole; at the grid's end, where a drag moves nothing, a row still mostly in view will do. */
        val lowest = floor - pitch / 2
        val lowestAtEnd = floor - pitch / 4
        val halfTile = (layout.tileHeight * pitch / 2).toInt()
        /* The rows whose tiles sit whole in view, by their centres, counted from the row the walk began on. */
        fun wholeRows(origin: Int): List<Pair<Int, Int>> =
            (0..(floor - origin) / pitch).map { it to origin + it * pitch }.filter { (_, centre) -> centre - halfTile >= grid.top }

        /* The centre of the row the walk began on, which the grid carries upward as it scrolls. */
        var origin = topRowCentre(lines, layout, frame)
            ?: return Outcome.Stopped(Outcome.Reason.STORAGE_NOT_OPEN, entries, "no tile numbers in the grid", rowsOf(lines))
        val (startRow, startColumn) = selectedTile(frame, layout, wholeRows(origin).map { it.second }) ?: (0 to 0)
        origin += startRow * pitch
        /* The framed tile, by its column and its centre's y, which the grid carries with it. */
        var framed: Pair<Int, Int>? = if (selectedTile(frame, layout, wholeRows(origin - startRow * pitch).map { it.second }) != null) startColumn to origin else null

        var row = 0
        var atEnd = false
        /* Drags since the last piece was read; a grid that keeps being dragged and never yields a row is lost. */
        var idleDrags = 0

        /*
         * The frame once the grid has stopped moving, and how far it moved: by the framed tile
         * where there is one, else by the words on the tiles. A list keeps gliding after the finger
         * lifts, so captures follow one another until two show the grid at the same place.
         */
        suspend fun landed(): Triple<Frame, List<Line>, Int?> {
            /* How far the grid moved: from where the framed tile was to where it is, else by the tiles' words. */
            fun shift(at: Int?, after: List<Line>): Int? = at?.let { framed!!.second - it } ?: shiftByText(lines, after, grid, columnPitch)
            fun framedIn(shot: Frame): Int? = framed?.let { (column, centre) -> framedCentre(shot, layout, column, grid.top + halfTile, minOf(centre + pitch / 2, floor)) }
            var shot = screen.capture()
            var shotLines = reader.read(shot)
            var at = framedIn(shot)
            repeat(GLIDES) {
                delay(settleMillis)
                val again = screen.capture()
                val againLines = reader.read(again)
                val atAgain = framedIn(again)
                val still = if (at != null && atAgain != null) abs(at - atAgain) <= STILL else shiftByText(shotLines, againLines, grid, columnPitch)?.let { abs(it) <= STILL } ?: false
                shot = again
                shotLines = againLines
                at = atAgain
                if (still) return Triple(shot, shotLines, shift(at, shotLines))
            }

            return Triple(shot, shotLines, shift(at, shotLines))
        }

        /* Drags the grid a row up and finds where it landed; null where the walk goes on, an outcome where it ends. */
        suspend fun advance(): Outcome? {
            if (++idleDrags > IDLE_DRAGS) return Outcome.Stopped(Outcome.Reason.GRID_LOST, entries, "$IDLE_DRAGS drags in a row brought no row within reach", rowsOf(lines))
            scroll(frame)
            val (moved, movedLines, shift) = landed()
            if (shift == null) {
                return Outcome.Stopped(
                    Outcome.Reason.GRID_LOST,
                    entries,
                    "neither the framed tile nor the tiles seen before the drag were found after it: ${tileWords(lines, grid).size} tile words before, ${tileWords(movedLines, grid).size} after",
                    rowsOf(movedLines),
                )
            }
            frame = moved
            lines = movedLines
            if (shift > 0) {
                origin -= shift
                framed = framed?.let { (column, centre) -> column to centre - shift }
            }
            /* At the grid's end a drag overscrolls and springs back; what is left of that is no move. */
            if (shift <= pitch / SPRING) {
                if (origin + row * pitch > lowestAtEnd) return Outcome.Finished(entries, rowsOf(lines))
                atEnd = true
            }

            return null
        }

        fun tilesOn(centreY: Int): List<Int> = (0 until layout.columns).filter { tileAt(lines, layout, frame, it, centreY) }
        /* Whether the row below this one would print its numbers inside the grid, were it there. */
        fun rowBelowInView(centreY: Int): Boolean = centreY + pitch + (layout.labelBelowCentre * pitch).toInt() + pitch / 4 <= grid.bottom

        while (true) {
            while (origin + row * pitch > lowest && !atEnd) advance()?.let { return it }

            /*
             * The storage fills every row but its last, so a number the recogniser missed mid-row is still
             * a tile. A row short of numbers is the last only where the row below shows none; where that
             * row is out of view the grid is dragged until it is, or until the grid moves no more.
             */
            var found = tilesOn(origin + row * pitch)
            while (found.isNotEmpty() && found.size < layout.columns && !atEnd && !rowBelowInView(origin + row * pitch)) {
                advance()?.let { return it }
                found = tilesOn(origin + row * pitch)
            }
            if (found.isEmpty()) return Outcome.Finished(entries, rowsOf(lines))
            val last = if (tilesOn(origin + row * pitch + pitch).isEmpty()) found.max() else layout.columns - 1

            for (column in (if (row == 0) startColumn else 0)..last) {
                /* The tap is checked by the frame it should leave, and where the frame sits is where the row is. */
                val centreY = origin + row * pitch
                val x = layout.tileX(column, frame.width)
                var at: Int? = null
                repeat(TAPS) {
                    if (at != null) return@repeat
                    touch.tap(x, centreY)
                    delay(settleMillis)
                    frame = screen.capture()
                    at = framedCentre(frame, layout, column, centreY - pitch / 2, centreY + pitch / 2)
                }
                val framedAt = at ?: return Outcome.Stopped(Outcome.Reason.GRID_LOST, entries, "tapped row $row column $column at $x,$centreY but no tile there is framed", rowsOf(reader.read(frame)))
                origin += framedAt - centreY
                framed = column to framedAt
                idleDrags = 0
                lines = reader.read(frame)
                val panel = rowsWithin(frame, lines, layout.panel)
                var entry = ScanEntry(entries.size, row, column, readGearCard(panel), panel)
                if (entry.unread) entry = entry.copy(png = keeper.keep(frame, entry))
                entries += entry
                progress(Progress(entries.size, held))
            }
            if (atEnd && tilesOn(origin + row * pitch + pitch).isEmpty()) return Outcome.Finished(entries, rowsOf(lines))
            row++
        }
    }

    /** Dragged slowly so the grid stops near where the finger does; where exactly is measured after. */
    private suspend fun scroll(frame: Frame) {
        val x = (layout.dragX * frame.width).toInt()
        val from = (layout.dragFromY * frame.height).toInt()
        touch.drag(x, from, x, from - layout.rowsPerDrag * layout.pitchY(frame.height), DRAG_MILLIS)
        delay(settleMillis * SPRING_SETTLES)
    }

    private fun rowsWithin(frame: Frame, lines: List<Line>, region: Region): List<String> {
        val box = region.box(frame.width, frame.height)

        return rowsOf(lines.filter { box.holds(it) })
    }

    private companion object {
        const val DRAG_MILLIS = 900L
        /** Captures given to a grid that keeps gliding before the walk takes the last as it is. */
        const val GLIDES = 6
        /** Pixels the tiles may move between two captures and still count as stopped. */
        const val STILL = 2
        /** Taps a tile gets before the walk gives up on it. */
        const val TAPS = 2
        /** The share of a row's pitch under which a drag is taken to have moved nothing. */
        const val SPRING = 8
        /** Settle times given to the spring-back after a drag before the first look. */
        const val SPRING_SETTLES = 3
        /** Drags without a piece read after which the grid is lost. */
        const val IDLE_DRAGS = 4
    }
}
