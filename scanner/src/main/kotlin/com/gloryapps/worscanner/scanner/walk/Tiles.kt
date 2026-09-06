package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.Frame
import com.gloryapps.worscanner.scanner.reading.Box
import com.gloryapps.worscanner.scanner.reading.Line
import kotlin.math.abs

/** A word printed on a tile, which is any word in the grid but the `+16` badge every tile carries. */
fun Line.namesATile(): Boolean = !text.trim().startsWith("+")

/**
 * The centre of the topmost row whose tiles sit whole in the viewport, read off the numbers the
 * tiles print: each number sits a fixed way below its tile's centre.
 */
fun topRowCentre(lines: List<Line>, layout: StorageLayout, frame: Frame): Int? {
    val grid = layout.grid.box(frame.width, frame.height)
    val pitch = layout.pitchY(frame.height)
    val above = (layout.labelBelowCentre * pitch).toInt()
    /* A row is whole once its tile's top edge clears the viewport's, which is half a tile below its centre. */
    val lowestTop = (layout.gridTop * frame.height).toInt() + (layout.tileHeight * pitch / 2).toInt()

    return lines.filter { grid.holds(it) && it.namesATile() }
        .map { it.box.top - above }
        .filter { it >= lowestTop }
        .minOrNull()
}

/**
 * Whether a tile is printed at this column and row centre: its number is there.
 *
 * The recogniser runs neighbouring numbers into one line when they sit level, so a line counts
 * for every column its box reaches over, not only the one it is centred on.
 */
fun tileAt(lines: List<Line>, layout: StorageLayout, frame: Frame, column: Int, centreY: Int): Boolean {
    val x = layout.tileX(column, frame.width)
    val labelY = centreY + (layout.labelBelowCentre * layout.pitchY(frame.height)).toInt()
    val slackX = layout.pitchX(frame.width) / 2
    val slackY = layout.pitchY(frame.height) / 4

    return lines.any { line ->
        line.namesATile() && x in (line.box.left - slackX)..(line.box.right + slackX) && abs(line.box.top - labelY) <= slackY
    }
}

/**
 * The tile the game has selected, by the light frame it draws on that tile's edge: the one whose
 * edge is far brighter than every other's. Null where no tile stands out.
 */
fun selectedTile(frame: Frame, layout: StorageLayout, rowCentres: List<Int>): Pair<Int, Int>? {
    val halfW = (layout.tileWidth * layout.pitchX(frame.width) / 2).toInt()
    val halfH = (layout.tileHeight * layout.pitchY(frame.height) / 2).toInt()
    val edges = rowCentres.indices.flatMap { row ->
        (0 until layout.columns).map { column ->
            Triple(row, column, edgeBrightness(frame, layout.tileX(column, frame.width), rowCentres[row], halfW, halfH))
        }
    }
    if (edges.isEmpty()) return null
    val brightest = edges.maxBy { it.third }
    val others = edges.filter { it !== brightest }.map { it.third }.sorted()
    val usual = others.getOrElse(others.size / 2) { 0 }

    return if (brightest.third >= FRAMED && brightest.third > usual * STANDS_OUT) brightest.first to brightest.second else null
}

private fun edgeBrightness(frame: Frame, centreX: Int, centreY: Int, halfW: Int, halfH: Int): Int {
    var sum = 0
    var count = 0
    for (step in 0 until EDGE_SAMPLES) {
        val along = (2 * step + 1).toDouble() / (2 * EDGE_SAMPLES)
        val x = centreX - halfW + (2 * halfW * along).toInt()
        val y = centreY - halfH + (2 * halfH * along).toInt()
        for ((sx, sy) in listOf(x to centreY - halfH, x to centreY + halfH, centreX - halfW to y, centreX + halfW to y)) {
            sum += frame.luminanceAt(sx.coerceIn(0, frame.width - 1), sy.coerceIn(0, frame.height - 1))
            count++
        }
    }

    return sum / count
}

private const val EDGE_SAMPLES = 8
/** Below this mean brightness an edge is nobody's frame. */
private const val FRAMED = 140
private const val STANDS_OUT = 1.6

/** Registration's grid box, shared so both read the same rectangle. */
fun StorageLayout.gridBox(frame: Frame): Box = grid.box(frame.width, frame.height)
