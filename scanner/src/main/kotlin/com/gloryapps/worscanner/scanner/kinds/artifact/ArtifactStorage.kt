package com.gloryapps.worscanner.scanner.kinds.artifact

import com.gloryapps.worscanner.scanner.scan.GridLayout
import com.gloryapps.worscanner.scanner.scan.Region

/**
 * Where Storage's Artifact tab puts things, measured inside LDPlayer at 1280x720 on
 * `ldplayer-artifacts-1280x720.json` under the tests and on screenshots of the same screen: the
 * selected tile's frame 84x124 px, columns 109 px apart from x 194, rows 130.5 px apart.
 */
val ARTIFACT_STORAGE = GridLayout(
    columns = 7,
    firstTileX = 0.1516,
    tilePitchX = 0.0852,
    tilePitchY = 0.1813,
    tileWidth = 0.771,
    tileHeight = 0.95,
    labelBelowCentre = 0.115,
    gridTop = 0.194,
    gridBottom = 0.854,
    grid = Region(0.113, 0.194, 0.707, 0.854),
    /* Below the artifact's art, where the overlay's sheet may hang. */
    panel = Region(0.734, 0.38, 0.98, 0.89),
    count = Region(0.60, 0.13, 0.72, 0.175),
    dragX = 0.40,
    dragFromY = 0.78,
    rowsPerDrag = 1,
)
