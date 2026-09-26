package com.gloryapps.worscanner.scanner.scan

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
fun labelledTileAt(lines: List<Line>, layout: GridLayout, frame: Frame, column: Int, centreY: Int): Boolean {
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
 * The framed tile anywhere between `highest` and `lowest`, by its column and its centre's height:
 * the one column where [framedCentre] finds the frame. Null where none does, or where several do
 * and the frame cannot be told from the art.
 */
fun framedTile(frame: Frame, layout: GridLayout, highest: Int, lowest: Int): Pair<Int, Int>? =
    (0 until layout.columns).mapNotNull { column -> framedCentre(frame, layout, column, highest, lowest)?.let { column to it } }.singleOrNull()

/**
 * Where the framed tile's centre now sits in its column, between `highest` and `lowest`: the
 * height at which that column's tile edge is far paler than the other columns' at the same
 * height, and palest of all such heights. The frame moves with the grid and no other tile wears
 * one, which makes it the one mark on the grid that cannot be mistaken for another, where the
 * tiles' numbers can. Null where no height shows a frame.
 */
fun framedCentre(frame: Frame, layout: GridLayout, column: Int, highest: Int, lowest: Int): Int? {
    var best: Pair<Int, Int>? = null
    for (y in highest..lowest step SCAN_STEP) {
        val paleness = edgePaleness(frame, layout.tileBox(frame, column, y))
        if (paleness < FRAMED || best != null && paleness <= best.second) continue
        val others = (0 until layout.columns).filter { it != column }.map { edgePaleness(frame, layout.tileBox(frame, it, y)) }.sorted()
        val usual = others.getOrElse(others.size / 2) { 0 }
        if (paleness > usual * STANDS_OUT) best = y to paleness
    }

    return best?.first
}

/* The frame is a line a few pixels wide, so each sample takes the palest pixel across the edge. */
private fun edgePaleness(frame: Frame, tile: Box): Int {
    var sum = 0
    var count = 0
    for (step in 0 until EDGE_SAMPLES) {
        val along = (2 * step + 1).toDouble() / (2 * EDGE_SAMPLES)
        val x = tile.left + ((tile.right - tile.left) * along).toInt()
        val y = tile.top + ((tile.bottom - tile.top) * along).toInt()
        for ((sx, sy, across) in listOf(Sample(x, tile.top, 0 to 1), Sample(x, tile.bottom, 0 to 1), Sample(tile.left, y, 1 to 0), Sample(tile.right, y, 1 to 0))) {
            sum += (-ACROSS..ACROSS).maxOf { frame.colourAt((sx + across.first * it).coerceIn(0, frame.width - 1), (sy + across.second * it).coerceIn(0, frame.height - 1)).paleness }
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

/** The tile's own rectangle on this frame, at its column and its centre's height; the selection frame is drawn on its edge. */
fun GridLayout.tileBox(frame: Frame, column: Int, centreY: Int): Box {
    val x = tileX(column, frame.width)
    val halfW = (tileWidth * pitchX(frame.width) / 2).toInt()
    val halfH = (tileHeight * pitchY(frame.height) / 2).toInt()

    return Box(x - halfW, centreY - halfH, x + halfW, centreY + halfH)
}
