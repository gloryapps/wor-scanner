package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.Frame
import com.gloryapps.worscanner.scanner.Screen
import com.gloryapps.worscanner.scanner.TextReader
import com.gloryapps.worscanner.scanner.Touch
import com.gloryapps.worscanner.scanner.reading.Box
import com.gloryapps.worscanner.scanner.reading.Line
import kotlin.math.roundToInt

/**
 * A storage screen made of numbers: `pieces` tiles in the layout's grid, a header with the count,
 * and a panel naming whichever tile was tapped last. Each tile's art is its index, as brightness.
 */
class FakeStorage(
    private val pieces: Int,
    private val layout: StorageLayout = StorageLayout(),
    /** Rows the grid actually moves on a drag, whole or not; the walk must not care. */
    private val rowsPerDrag: Double = layout.rowsPerDrag.toDouble(),
    private val storageOpen: Boolean = true,
    private val unreadable: Set<Int> = emptySet(),
    /** Rows the grid was left scrolled by before the walk began. */
    startRow: Int = 0,
) : Screen, Touch, TextReader {
    val taps = mutableListOf<Pair<Int, Int>>()
    var drags = 0
    /** How far the grid has scrolled, in pixels of a 1000-pixel display. */
    private var scrolled = startRow * (layout.tilePitchY * 1000).toInt()
    private var selected: Int? = null
    private val rows get() = (pieces + layout.columns - 1) / layout.columns
    private val pitch get() = (layout.tilePitchY * 1000).toInt()

    inner class Fake : Frame {
        override val width = 1000
        override val height = 1000
        val offset = scrolled
        val shown = selected

        /* A tile is its index as brightness, shaded top to bottom so only the true shift lines two frames up. */
        override fun luminanceAt(x: Int, y: Int): Int {
            val column = ((x / 1000.0 - layout.firstTileX) / layout.tilePitchX).roundToInt()
            val along = y + offset - layout.firstTileY * 1000
            val row = (along / pitch).roundToInt()
            if (column !in 0 until layout.columns || row < 0) return 0
            if (y < layout.gridTop * 1000 || y > layout.gridBottom * 1000) return 0
            val index = row * layout.columns + column
            val shade = ((along - row * pitch) / 2).toInt()

            return if (index < pieces) 60 + (index * 37) % 120 + shade else 0
        }
    }

    override suspend fun capture(): Frame = Fake()

    override suspend fun tap(x: Int, y: Int) {
        taps += x to y
        val column = ((x / 1000.0 - layout.firstTileX) / layout.tilePitchX).roundToInt()
        val row = ((y + scrolled - layout.firstTileY * 1000) / pitch).roundToInt()
        selected = row * layout.columns + column
    }

    /* The grid stops where its last row sits on the viewport's floor, as a list does; a drag down moves what it asks. */
    override suspend fun drag(fromX: Int, fromY: Int, toX: Int, toY: Int, millis: Long) {
        drags++
        val floor = (layout.firstTileY * 1000 + (rows - 1) * pitch - (layout.gridBottom * 1000 - pitch / 2)).toInt().coerceAtLeast(0)
        val by = if (toY > fromY) toY - fromY else -(rowsPerDrag * pitch).toInt()
        scrolled = (scrolled - by).coerceIn(0, floor)
    }

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
            val centre = (layout.firstTileY * 1000).toInt() + row * pitch - fake.offset
            if (centre < layout.gridTop * 1000 || centre > layout.gridBottom * 1000) continue
            for (column in 0 until layout.columns) {
                val index = row * layout.columns + column
                if (index >= pieces) break
                val x = layout.tileX(column, 1000)
                lines += Line("+16", Box(x + 10, centre - 60, x + 40, centre - 45))
                lines += Line("${1000 + (index * 7) % 9}", Box(x - 30, centre + 45, x + 30, centre + 60))
            }
        }
        /* The overlay's own words, outside every region the walk reads. */
        lines += Line("Scan Stop", Box(10, 10, 120, 30))

        return lines
    }

    private fun at(text: String, region: Region) = Line(text, region.box(1000, 1000))
}
