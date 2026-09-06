package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.Frame
import com.gloryapps.worscanner.scanner.Screen
import com.gloryapps.worscanner.scanner.TextReader
import com.gloryapps.worscanner.scanner.Touch
import com.gloryapps.worscanner.scanner.reading.Box
import com.gloryapps.worscanner.scanner.reading.Line
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A storage screen made of numbers: `pieces` tiles in the layout's grid, a header with the count,
 * and a panel naming whichever tile is selected. Every tile prints its badge and a number; the
 * selected tile has a light frame on its edge, the rest of the art is dark.
 */
class FakeStorage(
    private val pieces: Int,
    private val layout: StorageLayout = StorageLayout(),
    /** Rows the grid actually moves on a drag, whole or not; the walk must not care. */
    private val rowsPerDrag: Double = layout.rowsPerDrag.toDouble(),
    private val storageOpen: Boolean = true,
    private val unreadable: Set<Int> = emptySet(),
    /** How far the grid was left scrolled before the walk began, in rows, whole or not. */
    scrolledRows: Double = 0.0,
    /** The piece the game had selected before the walk began. */
    selected: Int? = null,
) : Screen, Touch, TextReader {
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

    private fun rowCentre(row: Int) = restingTop + row * pitch - scrolled

    inner class Fake : Frame {
        override val width = 1000
        override val height = 1000
        val shown = selected
        private val centres = (0 until rows).map { rowCentre(it) }

        override fun luminanceAt(x: Int, y: Int): Int {
            val chosen = shown ?: return DARK
            val cx = layout.tileX(chosen % layout.columns, 1000)
            val cy = centres[chosen / layout.columns]
            val halfW = (layout.tileWidth * pitchX / 2).toInt()
            val halfH = (layout.tileHeight * pitch / 2).toInt()
            val onEdge = (abs(abs(x - cx) - halfW) <= 2 && abs(y - cy) <= halfH) || (abs(abs(y - cy) - halfH) <= 2 && abs(x - cx) <= halfW)

            return if (onEdge) FRAME else DARK
        }
    }

    override suspend fun capture(): Frame = Fake()

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
    }

    override fun toString() = "FakeStorage(scrolled=$scrolled, selected=$selected)"

    override suspend fun read(frame: Frame): List<Line> {
        val fake = frame as Fake
        val lines = mutableListOf<Line>()
        if (storageOpen) lines += at("${pieces}/2,500", layout.count)
        fake.shown?.let { index ->
            val panel = layout.panel
            val step = (panel.bottom - panel.top) / 8
            val name = if (index in unreadable) "?? ??" else "Cataclysm Ring"
            lines += at(name, Region(panel.left, panel.top, panel.right, panel.top + step))
            lines += at("ATK Bonus ${index}%", Region(panel.left, panel.top + step, panel.right, panel.top + 2 * step))
            lines += at("Cataclysm", Region(panel.left, panel.top + 2 * step, panel.right, panel.top + 3 * step))
            lines += at("(3 pieces) Upon", Region(panel.left, panel.top + 3 * step, panel.right, panel.top + 4 * step))
        }
        /* Every tile in view carries its badge and its number, the way the game prints them. */
        for (row in 0 until rows) {
            val centre = rowCentre(row)
            if (centre < layout.gridTop * 1000 || centre > layout.gridBottom * 1000) continue
            for (column in 0 until layout.columns) {
                val index = row * layout.columns + column
                if (index >= pieces) break
                val x = layout.tileX(column, 1000)
                val labelTop = centre + (layout.labelBelowCentre * pitch).toInt()
                lines += Line("+16", Box(x + 10, centre - 60, x + 40, centre - 45))
                lines += Line("${1000 + (index * 7) % 9}", Box(x - 30, labelTop, x + 30, labelTop + 15))
            }
        }
        /* The overlay's own words, outside every region the walk reads. */
        lines += Line("Scan Stop", Box(10, 10, 120, 30))

        return lines
    }

    private fun at(text: String, region: Region) = Line(text, region.box(1000, 1000))

    private companion object {
        const val DARK = 40
        const val FRAME = 230
    }
}
