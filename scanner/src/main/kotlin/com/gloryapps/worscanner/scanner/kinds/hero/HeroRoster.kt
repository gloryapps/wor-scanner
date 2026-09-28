package com.gloryapps.worscanner.scanner.kinds.hero

import com.gloryapps.worscanner.scanner.scan.GridLayout
import com.gloryapps.worscanner.scanner.scan.Region
import com.gloryapps.worscanner.scanner.scan.Spot

/**
 * Where the hero screen puts things with its roster shown as cards, measured inside LDPlayer at
 * 1280x720 on `ldplayer-heroes-1280x720.json` under the tests and on screenshots of the same screen;
 * a second aspect ratio is still to be checked against it.
 */
val HERO_CARDS = GridLayout(
    columns = 3,
    firstTileX = 0.0543,
    tilePitchX = 0.0715,
    tilePitchY = 0.1857,
    tileWidth = 0.907,
    tileHeight = 0.95,
    labelBelowCentre = 0.213,
    grid = Region(0.02, 0.150, 0.235, 0.905),
    /* Every tab prints right of the grid's art and left of the tabs. */
    panel = Region(0.655, 0.03, 0.906, 0.99),
    count = Region(0.02, 0.10, 0.12, 0.145),
    dragX = 0.125,
    dragFromY = 0.78,
    rowsPerDrag = 1,
)

/**
 * The roster shown as squares, measured inside LDPlayer at 1280x720 on screenshots on 2026-09-28:
 * tiles 74x76 px, columns 92 px apart from x 71, rows 90.5 px apart. A square prints no word; its
 * stars sit where a card's level does.
 */
val HERO_SQUARES = HERO_CARDS.copy(
    firstTileX = 0.0555,
    tilePitchX = 0.0719,
    tilePitchY = 0.126,
    tileWidth = 0.815,
    tileHeight = 0.85,
    labelBelowCentre = 0.28,
)

/** The two buttons above the roster that show it as squares or as cards; the lit one is the view shown. */
internal val SQUARES_BUTTON = Spot(0.1813, 0.1222)
internal val CARDS_BUTTON = Spot(0.2211, 0.1222)

/**
 * Where a tile's rank is read off its left edge, clear of the icons at its top, the stars at its
 * bottom and the art within: across, in shares of its width from its left; down, in shares of its
 * height below its centre; and the saturation above which its colour is a rank's, not a grey's.
 */
internal class RankEdge(val across: ClosedFloatingPointRange<Double>, val down: ClosedFloatingPointRange<Double>, val saturated: Double)

/* A card's border runs 2 to 12 px in, past the selection frame on its edge: 0.51 saturated and up on a legendary or an epic, 0.49 at most on a common. */
internal val CARDS_EDGE = RankEdge(across = 0.025..0.145, down = 0.0..0.5, saturated = 0.45)
/* A square's is the paler line on its edge: 0.39 saturated and up on a legendary or an epic, 0.16 at most on a common. */
internal val SQUARES_EDGE = RankEdge(across = 0.0..0.05, down = -0.5..0.15, saturated = 0.3)

internal val ATTRIBUTES = Spot(0.953, 0.090)
internal val SKILLS = Spot(0.953, 0.493)
internal val AWAKEN = Spot(0.953, 0.639)

/** The six star slots under the level, each taken left of its centre, where a promoted star is purple and not the pale of its right half. */
internal val STAR_SLOTS = (0 until 6).map { Spot(0.7422 + 0.01719 * it, 0.7847) }

/** Awakened I to V down the Awaken tab, each on its node's ring. */
internal val AWAKENING_NODES = listOf(0.3375, 0.4694, 0.6028, 0.7347, 0.8681).map { Spot(0.6859, it) }
