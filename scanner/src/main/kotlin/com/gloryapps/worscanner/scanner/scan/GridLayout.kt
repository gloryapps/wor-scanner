package com.gloryapps.worscanner.scanner.scan

/**
 * Where a kind's screen puts things, in fractions of the display.
 *
 * The grid is `columns` wide and scrolls continuously beside the panel, inside the viewport `grid`;
 * where its rows sit at any moment is read off the frame, never assumed. Each kind measures its own
 * values off a recorded frame.
 */
data class GridLayout(
    val columns: Int,
    /** Centre of the first column. */
    val firstTileX: Double,
    val tilePitchX: Double,
    val tilePitchY: Double,
    /** The tile's own rectangle, as a fraction of the pitch; the selection frame is drawn on its edge. */
    val tileWidth: Double,
    val tileHeight: Double,
    /** How far below the tile's centre its number is printed, as a fraction of the row pitch. */
    val labelBelowCentre: Double,
    /** The grid's viewport, where the tiles' words are read; a row is tapped only while its centre sits well inside it. */
    val grid: Region,
    val panel: Region,
    /** Where the header prints `1,169/2,500`. */
    val count: Region,
    /** A drag from here upward, inside the grid, scrolls it. */
    val dragX: Double,
    val dragFromY: Double,
    /**
     * How many rows a drag asks for; what it gets is measured afterwards.
     *
     * One row keeps two rows of the three in view on both frames, so a drag that runs on a row
     * past the finger still leaves something to find it by.
     */
    val rowsPerDrag: Int,
) {
    fun tileX(column: Int, width: Int): Int = ((firstTileX + column * tilePitchX) * width).toInt()
    fun pitchY(height: Int): Int = (tilePitchY * height).toInt()
    fun pitchX(width: Int): Int = (tilePitchX * width).toInt()
    fun labelBelow(height: Int): Int = (labelBelowCentre * pitchY(height)).toInt()
    fun halfTile(height: Int): Int = (tileHeight * pitchY(height) / 2).toInt()
}
