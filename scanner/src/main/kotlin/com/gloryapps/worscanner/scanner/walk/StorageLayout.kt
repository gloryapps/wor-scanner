package com.gloryapps.worscanner.scanner.walk

/**
 * Where the storage screen puts things, in fractions of the display.
 *
 * The grid is `columns` wide and scrolls continuously under the panel on the right, inside the
 * viewport between `gridTop` and `gridBottom`; where its rows sit at any moment is read off the
 * frame, never assumed. The values were measured on a reading taken inside LDPlayer at 1280x720,
 * kept as `ldplayer-storage-1280x720.json` under the tests; a second aspect ratio is still to be
 * checked against them.
 */
data class StorageLayout(
    val columns: Int = 7,
    /** Centre of the first column. */
    val firstTileX: Double = 0.152,
    val tilePitchX: Double = 0.0848,
    val tilePitchY: Double = 0.184,
    /** The tile's own rectangle, as a fraction of the pitch; the selection frame is drawn on its edge. */
    val tileWidth: Double = 0.85,
    val tileHeight: Double = 0.90,
    /** How far below the tile's centre its number is printed, as a fraction of the row pitch. */
    val labelBelowCentre: Double = 0.28,
    /** The grid's viewport; a row is tapped only while its centre sits well inside it. */
    val gridTop: Double = 0.194,
    val gridBottom: Double = 0.854,
    /** The grid's own rectangle, where the tiles' words are read. */
    val grid: Region = Region(0.113, 0.194, 0.707, 0.854),
    val panel: Region = Region(0.734, 0.20, 0.98, 0.89),
    /** Where the header prints `1,169/2,500`. */
    val count: Region = Region(0.60, 0.13, 0.72, 0.175),
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
