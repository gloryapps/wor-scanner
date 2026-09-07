package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.kinds.GridLayout
import com.gloryapps.worscanner.scanner.kinds.holds
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import kotlin.math.abs

/** A word printed on a tile, which is any word in the grid but the `+16` badge every tile carries. */
fun Line.namesATile(): Boolean = !text.trim().startsWith("+")

/**
 * The centre of the topmost row whose tiles sit whole in the viewport, read off the numbers the
 * tiles print: each number sits a fixed way below its tile's centre.
 */
fun topRowCentre(lines: List<Line>, layout: GridLayout, frame: Frame): Int? {
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
fun tileAt(lines: List<Line>, layout: GridLayout, frame: Frame, column: Int, centreY: Int): Boolean {
    val grid = layout.gridBox(frame)
    val x = layout.tileX(column, frame.width)
    val labelY = centreY + (layout.labelBelowCentre * layout.pitchY(frame.height)).toInt()
    val slackX = layout.pitchX(frame.width) / 2
    val slackY = layout.pitchY(frame.height) / 4

    return lines.any { line ->
        grid.holds(line) && line.namesATile() && x in (line.box.left - slackX)..(line.box.right + slackX) && abs(line.box.top - labelY) <= slackY
    }
}

/**
 * The tile the game has selected, by the pale frame it draws on that tile's edge: the one whose
 * edge is far paler than every other's. Every tile has a coloured border of its own, red or gold,
 * as bright as the frame, so brightness tells nothing and paleness everything. Null where no tile
 * stands out.
 */
fun selectedTile(frame: Frame, layout: GridLayout, rowCentres: List<Int>): Pair<Int, Int>? {
    val halfW = (layout.tileWidth * layout.pitchX(frame.width) / 2).toInt()
    val halfH = (layout.tileHeight * layout.pitchY(frame.height) / 2).toInt()
    val edges = rowCentres.indices.flatMap { row ->
        (0 until layout.columns).map { column ->
            Triple(row, column, edgePaleness(frame, layout.tileX(column, frame.width), rowCentres[row], halfW, halfH))
        }
    }
    if (edges.isEmpty()) return null
    val brightest = edges.maxBy { it.third }
    val others = edges.filter { it !== brightest }.map { it.third }.sorted()
    val usual = others.getOrElse(others.size / 2) { 0 }

    return if (brightest.third >= FRAMED && brightest.third > usual * STANDS_OUT) brightest.first to brightest.second else null
}

/**
 * Where the framed tile's centre now sits in its column, between `highest` and `lowest`: the
 * height at which that column's tile edge is far paler than the other columns' at the same
 * height, and palest of all such heights. The frame moves with the grid and no other tile wears
 * one, which makes it the one mark on the grid that cannot be mistaken for another, where the
 * tiles' numbers can. Null where no height shows a frame.
 */
fun framedCentre(frame: Frame, layout: GridLayout, column: Int, highest: Int, lowest: Int): Int? {
    val halfW = (layout.tileWidth * layout.pitchX(frame.width) / 2).toInt()
    val halfH = (layout.tileHeight * layout.pitchY(frame.height) / 2).toInt()
    var best: Pair<Int, Int>? = null
    for (y in highest..lowest step SCAN_STEP) {
        val paleness = edgePaleness(frame, layout.tileX(column, frame.width), y, halfW, halfH)
        if (paleness < FRAMED || best != null && paleness <= best.second) continue
        val others = (0 until layout.columns).filter { it != column }.map { edgePaleness(frame, layout.tileX(it, frame.width), y, halfW, halfH) }.sorted()
        val usual = others.getOrElse(others.size / 2) { 0 }
        if (paleness > usual * STANDS_OUT) best = y to paleness
    }

    return best?.first
}

/* The frame is a line a few pixels wide, so each sample takes the palest pixel across the edge. */
private fun edgePaleness(frame: Frame, centreX: Int, centreY: Int, halfW: Int, halfH: Int): Int {
    var sum = 0
    var count = 0
    for (step in 0 until EDGE_SAMPLES) {
        val along = (2 * step + 1).toDouble() / (2 * EDGE_SAMPLES)
        val x = centreX - halfW + (2 * halfW * along).toInt()
        val y = centreY - halfH + (2 * halfH * along).toInt()
        for ((sx, sy, across) in listOf(Sample(x, centreY - halfH, 0 to 1), Sample(x, centreY + halfH, 0 to 1), Sample(centreX - halfW, y, 1 to 0), Sample(centreX + halfW, y, 1 to 0))) {
            sum += (-ACROSS..ACROSS).maxOf { frame.palenessAt((sx + across.first * it).coerceIn(0, frame.width - 1), (sy + across.second * it).coerceIn(0, frame.height - 1)) }
            count++
        }
    }

    return sum / count
}

private data class Sample(val x: Int, val y: Int, val across: Pair<Int, Int>)

private const val EDGE_SAMPLES = 8
/** Pixels between two heights tried for the frame. */
private const val SCAN_STEP = 2
/** Pixels looked at on either side of the edge for the frame's line. */
private const val ACROSS = 2
/** Below this mean paleness an edge is nobody's frame. */
private const val FRAMED = 140
private const val STANDS_OUT = 1.6

/** The grid's box on this frame, shared so every reader of the grid reads the same rectangle. */
fun GridLayout.gridBox(frame: Frame): Box = grid.box(frame.width, frame.height)
