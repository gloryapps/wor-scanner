package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.senses.Screen
import com.gloryapps.worscanner.scanner.senses.TextReader
import com.gloryapps.worscanner.scanner.senses.Touch
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import com.gloryapps.worscanner.scanner.text.rowsOf
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One run of a scan over its kind's grid: tap a tile, let the kind read it, next tile; drag when
 * the next row sits too low to tap and find where the grid landed by the framed tile, else by the
 * words on its tiles.
 *
 * It begins on the tile the game has selected, or on the first whole row in view where none is,
 * and ends where the rows run out. A tile's identity is its place in the walk, never its content.
 */
internal class Walk<T>(
    private val scan: Scan<T>,
    private val screen: Screen,
    private val touch: Touch,
    private val reader: TextReader,
    private val keeper: Keeper<T>,
    private val settleMillis: Long,
    private val entries: MutableList<ScanEntry<T>>,
    private val progress: suspend (Progress) -> Unit,
) {
    private val layout = scan.layout
    /** The last frame looked at. */
    private lateinit var seen: Seen
    private lateinit var metrics: Metrics
    /** What the header counts. */
    private var held = 0
    /** The centre of the row the walk began on, which the grid carries upward as it scrolls. */
    private var origin = 0
    /** The framed tile, by its column and its centre's y, which the grid carries with it. */
    private var framed: Pair<Int, Int>? = null
    private var row = 0
    private var startColumn = 0
    private var atEnd = false
    /** Drags since the last tile was read; a grid that keeps being dragged and never yields a row is lost. */
    private var idleDrags = 0
    /** Drags in a row that moved nothing; one may have been dropped by the game, two is the grid's end. */
    private var stillDrags = 0

    private val centreY: Int get() = origin + row * metrics.pitch

    suspend fun run(): Outcome<T> {
        seen = look()
        held = countIn(seen.rowsIn(layout.count)) ?: return stopped(Outcome.Reason.STORAGE_NOT_OPEN, "no count like 1,169/2,500 in the header")
        metrics = Metrics(layout, seen.frame)
        begin()?.let { return it }

        while (true) {
            while (centreY > metrics.lowest && !atEnd) advance()?.let { return it }

            /*
             * The storage fills every row but its last, so a number the recogniser missed mid-row is still
             * a tile. A row short of numbers is the last only where the row below shows none; where that
             * row is out of view the grid is dragged until it is, or until the grid moves no more.
             */
            var found = tilesOn(centreY)
            while (found.isNotEmpty() && found.size < layout.columns && !atEnd && !rowBelowInView(centreY)) {
                advance()?.let { return it }
                found = tilesOn(centreY)
            }
            if (found.isEmpty()) return finished()
            val last = if (tilesOn(centreY + metrics.pitch).isEmpty()) found.max() else layout.columns - 1

            for (column in (if (row == 0) startColumn else 0)..last) {
                tap(column)?.let { return it }
                read(column)?.let { return it }
            }
            if (atEnd && tilesOn(centreY + metrics.pitch).isEmpty()) return finished()
            row++
        }
    }

    /* Finds the tile to begin on: the one the game has framed, found anywhere a whole tile sits, else the first of the first whole row the kind finds. */
    private fun begin(): Outcome<T>? {
        val selected = framedTile(seen.frame, layout, metrics.grid.top + metrics.halfTile, metrics.floor)
        origin = selected?.second ?: scan.firstRowCentre(seen) ?: return stopped(Outcome.Reason.STORAGE_NOT_OPEN, "no tile framed, and no row to begin on without one")
        startColumn = selected?.first ?: 0
        framed = selected

        return null
    }

    /* Taps the tile, checked by the frame it should leave; where the frame sits is where the row is. */
    private suspend fun tap(column: Int): Outcome<T>? {
        val y = centreY
        val x = layout.tileX(column, seen.frame.width)
        var shot = seen.frame
        var at: Int? = null
        repeat(TAPS) {
            if (at != null) return@repeat
            touch.tap(x, y)
            delay(settleMillis)
            shot = screen.capture()
            at = framedCentre(shot, layout, column, y - metrics.pitch / 2, y + metrics.pitch / 2)
        }
        seen = Seen(shot, reader.read(shot))
        val framedAt = at ?: return stopped(Outcome.Reason.GRID_LOST, "tapped row $row column $column at $x,$y but no tile there is framed")
        origin += framedAt - y
        framed = column to framedAt
        idleDrags = 0

        return null
    }

    /* Lets the kind read the tile just tapped; null where the walk goes on, an outcome where the kind ends it. */
    private suspend fun read(column: Int): Outcome<T>? {
        when (val tile = scan.readTile(Lent(seen, layout.tileBox(seen.frame, column, centreY)))) {
            is Read.Card -> keep(tile, column)
            Read.Beyond -> return finished()
            is Read.Lost -> return stopped(Outcome.Reason.GRID_LOST, tile.detail)
        }

        return null
    }

    /* Keeps what the kind read off the tile, its frames as an image where the record is not closed. */
    private suspend fun keep(tile: Read.Card<T>, column: Int) {
        var entry = ScanEntry(entries.size, row, column, tile.card, tile.rows)
        if (!tile.closed) entry = entry.copy(png = keeper.panel(tile.frames, entry.index))
        entries += entry
        keeper.entry(entry)
        progress(Progress(entries.size, held))
    }

    /* Once a kind's taps have moved the grid, waits for it to stop and takes its place from the framed tile, found anywhere in its column. */
    private suspend fun regrip(): Seen? {
        val (settled, shift) = landed(reach = metrics.floor)
        seen = settled
        if (shift == null) return null
        origin -= shift
        framed = framed?.let { (column, centre) -> column to centre - shift }

        return settled
    }

    /* Drags the grid a row up and finds where it landed; null where the walk goes on, an outcome where it ends. */
    private suspend fun advance(): Outcome<T>? {
        if (++idleDrags > IDLE_DRAGS) return stopped(Outcome.Reason.GRID_LOST, "$IDLE_DRAGS drags in a row brought no row within reach")
        scroll()
        val (moved, shift) = landed()
        if (shift == null) {
            val words = "${tileWords(seen.lines, metrics.grid).size} tile words before, ${tileWords(moved.lines, metrics.grid).size} after"

            return stopped(Outcome.Reason.GRID_LOST, "neither the framed tile nor the tiles seen before the drag were found after it: $words", moved)
        }
        seen = moved
        if (shift > 0) {
            origin -= shift
            framed = framed?.let { (column, centre) -> column to centre - shift }
        }
        /* At the grid's end a drag overscrolls and springs back; what is left of that is no move. */
        stillDrags = if (shift <= metrics.pitch / SPRING) stillDrags + 1 else 0
        if (stillDrags >= STILL_DRAGS) {
            if (centreY > metrics.lowestAtEnd) return finished()
            atEnd = true
        }

        return null
    }

    /*
     * The frame once the grid has stopped moving, and how far it moved: by the framed tile, looked
     * for up to `reach` below where it was, else by the words on the tiles. A list keeps gliding
     * after the finger lifts, so captures follow one another until two show the grid at the same place;
     * while the framed tile is in view its pixels tell, and only the frame the grid settled on is read.
     */
    private suspend fun landed(reach: Int = metrics.pitch / 2): Pair<Seen, Int?> {
        fun framedIn(shot: Frame): Int? =
            framed?.let { (column, centre) -> framedCentre(shot, layout, column, metrics.grid.top + metrics.halfTile, minOf(centre + reach, metrics.floor)) }
        /* How far the grid moved: from where the framed tile was to where it is, else by the tiles' words. */
        fun shift(at: Int?, after: Seen): Int? = at?.let { framed!!.second - it } ?: shiftByText(seen.lines, after.lines, metrics.grid, metrics.columnPitch, metrics.pitch)
        var shot = Glance(screen.capture())
        var at = framedIn(shot.frame)
        repeat(GLIDES) {
            delay(settleMillis)
            val again = Glance(screen.capture())
            val atAgain = framedIn(again.frame)
            val still = if (at != null && atAgain != null) abs(at - atAgain) <= metrics.still else shiftByText(shot.lines(), again.lines(), metrics.grid, metrics.columnPitch, metrics.pitch)?.let { abs(it) <= metrics.still } ?: false
            shot = again
            at = atAgain
            if (still) return shot.seen().let { it to shift(at, it) }
        }

        return shot.seen().let { it to shift(at, it) }
    }

    /* Held still before it lifts, so the grid takes no fling and stops near where the finger does; where exactly is measured after. */
    private suspend fun scroll() {
        val x = (layout.dragX * seen.frame.width).toInt()
        val from = (layout.dragFromY * seen.frame.height).toInt()
        touch.drag(x, from, x, from - layout.rowsPerDrag * metrics.pitch, DRAG_MILLIS)
        delay(settleMillis)
    }

    private suspend fun look(): Seen = screen.capture().let { Seen(it, reader.read(it)) }

    /** A capture read by the recogniser only once its lines are asked for. */
    private inner class Glance(val frame: Frame) {
        private var read: List<Line>? = null

        suspend fun lines(): List<Line> = read ?: reader.read(frame).also { read = it }

        suspend fun seen(): Seen = Seen(frame, lines())
    }

    /* What the walk lends the kind for the tile just tapped: its eyes and hand, and the grid's place. */
    private inner class Lent(override val seen: Seen, override val tile: Box) : Tapped {
        /** What the panel showed last, which a tab must change. */
        private var shown = seen

        override suspend fun show(tab: Spot): Seen {
            val before = shown.rowsIn(layout.panel)
            repeat(TAPS) {
                val (x, y) = tab.on(shown.frame)
                touch.tap(x, y)
                /* A tab draws its panel over a few frames, blank at first: once it reads something new, it is given one more settle and read. */
                repeat(GLIDES) {
                    delay(settleMillis)
                    shown = look()
                    val rows = shown.rowsIn(layout.panel)
                    if (rows.isNotEmpty() && rows != before) {
                        delay(settleMillis)
                        shown = look()

                        return shown
                    }
                }
            }

            return shown
        }

        override suspend fun regrip(): Seen? = this@Walk.regrip()
    }

    private fun tilesOn(centreY: Int): List<Int> = (0 until layout.columns).filter { scan.tileAt(seen, it, centreY) }

    /* Whether the row below this one would print its numbers inside the grid, were it there. */
    private fun rowBelowInView(centreY: Int): Boolean =
        centreY + metrics.pitch + metrics.labelBelow + metrics.pitch / 4 <= metrics.grid.bottom

    private fun finished(): Outcome<T> = Outcome.Finished(entries, rowsOf(seen.lines))

    private fun stopped(reason: Outcome.Reason, detail: String, at: Seen = seen): Outcome<T> = Outcome.Stopped(reason, entries, detail, rowsOf(at.lines))

    /** Where the grid sits on this display, measured once off the first frame. */
    private class Metrics(layout: GridLayout, frame: Frame) {
        val pitch = layout.pitchY(frame.height)
        val grid = layout.gridBox(frame)
        val floor = grid.bottom
        val columnPitch = layout.pitchX(frame.width)
        /* A row is tapped where it sits whole; at the grid's end, where a drag moves nothing, a row still mostly in view will do. */
        val lowest = floor - pitch / 2
        val lowestAtEnd = floor - pitch / 4
        val halfTile = layout.halfTile(frame.height)
        val labelBelow = layout.labelBelow(frame.height)
        /** Pixels the tiles may move between two captures and still count as stopped. */
        val still = (pitch * STILL).roundToInt()
    }

    private companion object {
        const val DRAG_MILLIS = 450L
        /** Captures given to a grid that keeps gliding before the walk takes the last as it is. */
        const val GLIDES = 6
        /** The share of a row's pitch the tiles may move between two captures and still count as stopped. */
        const val STILL = 0.015
        /** Taps a tile or a tab gets before the walk gives up on it. */
        const val TAPS = 2
        /** The share of a row's pitch under which a drag is taken to have moved nothing. */
        const val SPRING = 8
        /** Drags in a row that moved nothing before the grid is taken to have ended. */
        const val STILL_DRAGS = 2
        /** Drags without a tile read after which the grid is lost. */
        const val IDLE_DRAGS = 4
    }
}
