package com.gloryapps.worscanner.scanner.gear

import com.gloryapps.worscanner.scanner.kind.GridLayout
import com.gloryapps.worscanner.scanner.kind.Region

/**
 * Where the gear storage puts things, measured on a reading taken inside LDPlayer at 1280x720
 * and kept as `ldplayer-storage-1280x720.json` under the tests; a second aspect ratio is still
 * to be checked against it.
 */
val GEAR_STORAGE = GridLayout(
    columns = 7,
    firstTileX = 0.152,
    tilePitchX = 0.0848,
    tilePitchY = 0.184,
    tileWidth = 0.85,
    tileHeight = 0.94,
    labelBelowCentre = 0.28,
    gridTop = 0.194,
    gridBottom = 0.854,
    grid = Region(0.113, 0.194, 0.707, 0.854),
    panel = Region(0.734, 0.20, 0.98, 0.89),
    count = Region(0.60, 0.13, 0.72, 0.175),
    dragX = 0.40,
    dragFromY = 0.78,
    rowsPerDrag = 1,
)
