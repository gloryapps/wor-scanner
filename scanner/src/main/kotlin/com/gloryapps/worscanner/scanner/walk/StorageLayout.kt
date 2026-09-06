package com.gloryapps.worscanner.scanner.walk

/**
 * Where the storage screen puts things, in fractions of the display.
 *
 * The grid is `columns` wide and scrolls continuously under the panel on the right, inside the
 * viewport between `gridTop` and `gridBottom`; where its rows sit at any moment is read off the
 * frame, never assumed. The values here were measured off one LDPlayer screenshot with the
 * emulator's frame cut away and are provisional until a reading from inside Android.
 */
data class StorageLayout(
    val columns: Int = 7,
    /** Centre of the first column. */
    val firstTileX: Double = 0.174,
    val tilePitchX: Double = 0.080,
    val tilePitchY: Double = 0.186,
    /** The tile's own rectangle, as a fraction of the pitch; the selection frame is drawn on its edge. */
    val tileWidth: Double = 0.90,
    val tileHeight: Double = 0.95,
    /** How far below the tile's centre its number is printed, as a fraction of the row pitch. */
    val labelBelowCentre: Double = 0.37,
    /** The grid's viewport; a row is tapped only while its centre sits well inside it. */
    val gridTop: Double = 0.195,
    val gridBottom: Double = 0.856,
    /** The grid's own rectangle, where the tiles' words are read. */
    val grid: Region = Region(0.11, 0.195, 0.70, 0.856),
    val panel: Region = Region(0.724, 0.195, 0.962, 0.904),
    /** Where the header prints `1,169/2,500`. */
    val count: Region = Region(0.59, 0.135, 0.705, 0.171),
    /** A drag from here upward, inside the grid, scrolls it. */
    val dragX: Double = 0.40,
    val dragFromY: Double = 0.78,
    /**
     * How many rows a drag asks for; what it gets is measured afterwards.
     *
     * One row keeps two rows of the three in view on both frames, so a drag that runs on a row
     * past the finger still leaves something to find it by.
     */
    val rowsPerDrag: Int = 1,
) {
    fun tileX(column: Int, width: Int): Int = ((firstTileX + column * tilePitchX) * width).toInt()
    fun pitchY(height: Int): Int = (tilePitchY * height).toInt()
    fun pitchX(width: Int): Int = (tilePitchX * width).toInt()
}
