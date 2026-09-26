package com.gloryapps.worscanner.scanner.kinds.hero

import com.gloryapps.worscanner.scanner.scan.GridLayout
import com.gloryapps.worscanner.scanner.scan.Region
import com.gloryapps.worscanner.scanner.scan.Spot

/**
 * Where the hero screen puts things, measured inside LDPlayer at 1280x720 on
 * `ldplayer-heroes-1280x720.json` under the tests and on screenshots of the same screen; a second
 * aspect ratio is still to be checked against it.
 */
val HERO_ROSTER = GridLayout(
    columns = 3,
    firstTileX = 0.0543,
    tilePitchX = 0.0715,
    tilePitchY = 0.1857,
    tileWidth = 0.907,
    tileHeight = 0.95,
    labelBelowCentre = 0.213,
    gridTop = 0.150,
    gridBottom = 0.905,
    grid = Region(0.02, 0.150, 0.235, 0.905),
    /* Every tab prints right of the grid's art and left of the tabs. */
    panel = Region(0.655, 0.03, 0.906, 0.99),
    count = Region(0.02, 0.10, 0.12, 0.145),
    dragX = 0.125,
    dragFromY = 0.78,
    rowsPerDrag = 1,
)

internal val ATTRIBUTES = Spot(0.953, 0.090)
internal val SKILLS = Spot(0.953, 0.493)
internal val AWAKEN = Spot(0.953, 0.639)

/** The six star slots under the level, each taken left of its centre, where a promoted star is purple and not the pale of its right half. */
internal val STAR_SLOTS = (0 until 6).map { Spot(0.7422 + 0.01719 * it, 0.7847) }

/** Awakened I to V down the Awaken tab, each on its node's ring. */
internal val AWAKENING_NODES = listOf(0.3375, 0.4694, 0.6028, 0.7347, 0.8681).map { Spot(0.6859, it) }
