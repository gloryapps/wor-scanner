package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.kinds.gear.GEAR_STORAGE
import com.gloryapps.worscanner.scanner.kinds.GridLayout
import com.gloryapps.worscanner.scanner.kinds.Reader
import com.gloryapps.worscanner.scanner.kinds.Region
import com.gloryapps.worscanner.scanner.kinds.Scannable
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.senses.Screen
import com.gloryapps.worscanner.scanner.senses.TextReader
import com.gloryapps.worscanner.scanner.senses.Touch
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import kotlinx.serialization.builtins.serializer
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A storage screen made of numbers, and the kind that reads it: `pieces` tiles in the layout's
 * grid, a header with the count, and a panel naming whichever tile is selected by its number.
 * Every tile prints its badge and a number; the selected tile has a light frame on its edge, the
 * rest of the art is dark. The record is the number, so the scan is proven over no real kind.
 */
class FakeStorage(
    private val pieces: Int,
    override val layout: GridLayout = GEAR_STORAGE,
    /** Rows the grid actually moves on a drag, whole or not; the scan must not care. */
    private val rowsPerDrag: Double = layout.rowsPerDrag.toDouble(),
    private val storageOpen: Boolean = true,
    private val unreadable: Set<Int> = emptySet(),
    /** How far the grid was left scrolled before the scan began, in rows, whole or not. */
    scrolledRows: Double = 0.0,
    /** The piece the game had selected before the scan began. */
    selected: Int? = null,
    /** Whether the recogniser hands a row's numbers back as one line, as it does when they sit level. */
    private val runsNumbersTogether: Boolean = false,
    /** Pieces whose number the recogniser never reads, though the tile is there. */
    private val unlabelled: Set<Int> = emptySet(),
    /** Rows the grid keeps gliding after a drag, one capture at a time, before it comes to rest. */
    private val glideRows: Double = 0.0,
    /** Whether every tile prints the same number, as a storage full of one stat does. */
    private val sameNumbers: Boolean = false,
) : Screen, Touch, TextReader, Scannable<Int> {
    override val serializer = Int.serializer()
    override val reader = object : Reader<Int> {
        override fun read(rows: List<String>): Int = rows.single().substringAfter("Piece ").toIntOrNull() ?: -1
        override fun closed(record: Int): Boolean = record >= 0
    }
    val taps = mutableListOf<Pair<Int, Int>>()
    var drags = 0
    private val pitch = (layout.tilePitchY * 1000).toInt()
    private val pitchX = (layout.tilePitchX * 1000).toInt()
    /** Where row zero's centre sits when the grid is at its top. */
    private val restingTop = (layout.gridTop * 1000).toInt() + pitch / 2 + 10
    private val rows get() = (pieces + layout.columns - 1) / layout.columns
    private val floor get() = (restingTop + (rows - 1) * pitch - ((layout.gridBottom * 1000).toInt() - pitch / 2)).coerceAtLeast(0)
    /** How far the grid has scrolled, in pixels of a 1000-pixel display. */
    private var scrolled = (scrolledRows * pitch).toInt().coerceIn(0, floor)
    private var selected: Int? = selected
    /** Pixels the grid has still to glide, given up a step per capture. */
    private var gliding = 0

    private fun rowCentre(row: Int) = restingTop + row * pitch - scrolled

    inner class Fake : Frame {
        override val width = 1000
        override val height = 1000
        val shown = selected
        private val centres = (0 until rows).map { rowCentre(it) }

        override fun palenessAt(x: Int, y: Int): Int {
            val chosen = shown ?: return DARK
            val cx = layout.tileX(chosen % layout.columns, 1000)
            val cy = centres[chosen / layout.columns]
            val halfW = (layout.tileWidth * pitchX / 2).toInt()
            val halfH = (layout.tileHeight * pitch / 2).toInt()
            val onEdge = (abs(abs(x - cx) - halfW) <= 2 && abs(y - cy) <= halfH) || (abs(abs(y - cy) - halfH) <= 2 && abs(x - cx) <= halfW)

            return if (onEdge) FRAME else DARK
        }
    }

    override suspend fun capture(): Frame {
        if (gliding > 0) {
            val step = minOf(gliding, pitch / 3)
            scrolled = (scrolled + step).coerceAtMost(floor)
            gliding -= step
        }

        return Fake()
    }

    override suspend fun tap(x: Int, y: Int) {
        taps += x to y
        val column = ((x - layout.firstTileX * 1000) / pitchX).roundToInt()
        val row = ((y + scrolled - restingTop).toDouble() / pitch).roundToInt()
        selected = row * layout.columns + column
    }

    /* The grid stops where its last row sits on the viewport's floor, as a list does. */
    override suspend fun drag(fromX: Int, fromY: Int, toX: Int, toY: Int, millis: Long) {
        drags++
        scrolled = (scrolled + (rowsPerDrag * pitch).toInt()).coerceAtMost(floor)
        gliding = (glideRows * pitch).toInt()
    }

    override fun toString() = "FakeStorage(scrolled=$scrolled, selected=$selected)"

    override suspend fun read(frame: Frame): List<Line> {
        val fake = frame as Fake
        val lines = mutableListOf<Line>()
        if (storageOpen) lines += at("${pieces}/2,500", layout.count)
        fake.shown?.let { index ->
            val panel = layout.panel
            lines += at(if (index in unreadable) "Piece ??" else "Piece $index", Region(panel.left, panel.top, panel.right, panel.top + (panel.bottom - panel.top) / 8))
        }
        /* Every tile in view carries its badge and its number, the way the game prints them. */
        for (row in 0 until rows) {
            val centre = rowCentre(row)
            if (centre < layout.gridTop * 1000 || centre > layout.gridBottom * 1000) continue
            val labelTop = centre + (layout.labelBelowCentre * pitch).toInt()
            val numbers = mutableListOf<Line>()
            for (column in 0 until layout.columns) {
                val index = row * layout.columns + column
                if (index >= pieces) break
                val x = layout.tileX(column, 1000)
                lines += Line("+16", Box(x + 10, centre - 60, x + 40, centre - 45))
                if (index in unlabelled) continue
                numbers += Line(if (sameNumbers) "66%" else "${1000 + (index * 7) % 9}", Box(x - 30, labelTop, x + 30, labelTop + 15))
            }
            if (runsNumbersTogether && numbers.isNotEmpty()) {
                lines += Line(numbers.joinToString(" ") { it.text }, Box(numbers.first().box.left, labelTop, numbers.last().box.right, labelTop + 15))
            } else {
                lines += numbers
            }
        }
        /* The overlay's own words, outside every region the scan reads. */
        lines += Line("Scan Stop", Box(10, 10, 120, 30))

        return lines
    }

    private fun at(text: String, region: Region) = Line(text, region.box(1000, 1000))

    private companion object {
        const val DARK = 40
        const val FRAME = 230
    }
}
