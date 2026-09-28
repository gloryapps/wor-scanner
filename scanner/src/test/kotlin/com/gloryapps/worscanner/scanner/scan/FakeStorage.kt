package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.senses.Colour
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.senses.Screen
import com.gloryapps.worscanner.scanner.senses.TextReader
import com.gloryapps.worscanner.scanner.senses.Touch
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import kotlinx.serialization.builtins.serializer
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A storage screen made of numbers: `pieces` tiles in the layout's grid, a header with the count,
 * and a panel naming whichever tile is selected by its number, under whichever tab is chosen where
 * there are tabs. Every tile prints its badge and a number; the selected tile has a light frame on
 * its edge, the rest of the art is dark.
 */
class FakeStorage(
    private val pieces: Int,
    val layout: GridLayout = FAKE_GRID,
    /** Rows the grid actually moves on a drag, whole or not; the scan must not care. */
    private val rowsPerDrag: Double = layout.rowsPerDrag.toDouble(),
    private val storageOpen: Boolean = true,
    private val unreadable: Set<Int> = emptySet(),
    /** How far the grid was left scrolled before the scan began, in rows, whole or not. */
    scrolledRows: Double = 0.0,
    /** The piece the game had selected before the scan began. */
    selected: Int? = null,
    /** Whether the recogniser hands a row's numbers back as one line, as it does when they sit level. */
    private val runsNumbersTogether: Boolean = false,
    /** Pieces whose number the recogniser never reads, though the tile is there. */
    private val unlabelled: Set<Int> = emptySet(),
    /** Rows the grid keeps gliding after a drag, one capture at a time, before it comes to rest. */
    private val glideRows: Double = 0.0,
    /** Whether every tile prints the same number, as a storage full of one stat does. */
    private val sameNumbers: Boolean = false,
    /** Drags, counted from one, that the game drops: the finger moves, the grid does not. */
    private val droppedDrags: Set<Int> = emptySet(),
    /** Tabs down the display's right edge; each switch re-centres the grid on the selected piece, as the hero screen's do. */
    tabCount: Int = 0,
    /** A tab under which the grid lists its own tiles, the first piece left out, as Awaken lists only the heroes that awaken. */
    private val ownListTab: Int? = null,
    /** Tab taps, counted from one, that the game drops: the panel stays as it was. */
    private val droppedTabTaps: Set<Int> = emptySet(),
    /** A tab tap, counted from one, after which the game shows another screen altogether. */
    private val leavesOnTabTap: Int? = null,
    /** Captures after a switch in which the panel is still blank, the game drawing it. */
    private val drawsTabsOver: Int = 0,
    /** Whether tiles show a coloured face and print only a badge the recogniser misreads, as the artifacts' do. */
    private val wordless: Boolean = false,
    /** Tile taps, counted from one, that the game drops: the frame stays where it was. */
    private val droppedTileTaps: Set<Int> = emptySet(),
    /** A drag, counted from one, after which the game shows another screen altogether. */
    private val leavesOnDrag: Int? = null,
    /** A capture, counted from one, that fails as a display that delivers no frame does. */
    private val failsOnCapture: Int? = null,
) : Screen, Touch, TextReader {
    val tabs = (0 until tabCount).map { Spot(TAB_X, 0.10 + 0.08 * it) }
    val taps = mutableListOf<Pair<Int, Int>>()
    var drags = 0
    /** Frames handed to the recogniser. */
    var reads = 0
    private var tabTaps = 0
    private var tileTaps = 0
    private var captures = 0
    private var tab = 0
    private var away = false
    /** Captures left before a switched tab's panel is drawn. */
    private var drawing = 0
    private val pitch = (layout.tilePitchY * 1000).toInt()
    private val pitchX = (layout.tilePitchX * 1000).toInt()
    /** Where row zero's centre sits when the grid is at its top. */
    private val restingTop = (layout.grid.top * 1000).toInt() + pitch / 2 + 10
    /** Pieces the current list leaves out at its start. */
    private val skipped get() = if (tab == ownListTab) 1 else 0
    private val rows get() = (pieces - skipped + layout.columns - 1) / layout.columns
    private val floor get() = (restingTop + (rows - 1) * pitch - ((layout.grid.bottom * 1000).toInt() - pitch / 2)).coerceAtLeast(0)
    /** How far the grid has scrolled, in pixels of a 1000-pixel display. */
    private var scrolled = (scrolledRows * pitch).toInt().coerceIn(0, floor)
    private var selected: Int? = selected
    /** Pixels the grid has still to glide, given up a step per capture. */
    private var gliding = 0

    private fun rowCentre(row: Int) = restingTop + row * pitch - scrolled

    /** Where the selected piece sits in the current list, if it is listed. */
    private fun selectedPlace(): Int? = selected?.minus(skipped)?.takeIf { it >= 0 }

    inner class Fake : Frame {
        override val width = 1000
        override val height = 1000
        val shown = selected
        val tab = this@FakeStorage.tab
        val away = this@FakeStorage.away
        val blank = drawing > 0
        private val place = if (away) null else selectedPlace()
        private val centres = (0 until rows).map { rowCentre(it) }

        private val halfW = (layout.tileWidth * pitchX / 2).toInt()
        private val halfH = layout.halfTile(1000)

        override fun colourAt(x: Int, y: Int): Colour = when {
            place?.let { onEdgeOf(it, x, y) } == true -> FRAME
            wordless && faceAt(x, y) -> FACE
            else -> DARK
        }

        private fun onEdgeOf(index: Int, x: Int, y: Int): Boolean {
            val cx = layout.tileX(index % layout.columns, 1000)
            val cy = centres[index / layout.columns]

            return (abs(abs(x - cx) - halfW) <= 2 && abs(y - cy) <= halfH) || (abs(abs(y - cy) - halfH) <= 2 && abs(x - cx) <= halfW)
        }

        /* Inside a listed tile, clear of the edge its frame is drawn on and of the grid's viewport. */
        private fun faceAt(x: Int, y: Int): Boolean {
            if (y < layout.grid.top * 1000 || y > layout.grid.bottom * 1000) return false
            val column = ((x - layout.firstTileX * 1000) / pitchX).roundToInt()
            val row = centres.indexOfFirst { abs(y - it) < halfH - 3 }
            return column in 0 until layout.columns && row >= 0 && abs(x - layout.tileX(column, 1000)) < halfW - 3 && row * layout.columns + column < pieces - skipped
        }
    }

    override suspend fun capture(): Frame {
        check(++captures != failsOnCapture) { "the display delivered no frame" }
        if (gliding > 0) {
            val step = minOf(gliding, pitch / 3)
            scrolled = (scrolled + step).coerceAtMost(floor)
            gliding -= step
        }

        return Fake().also { if (drawing > 0) drawing-- }
    }

    override suspend fun tap(x: Int, y: Int) {
        taps += x to y
        if (x >= (TAB_X * 1000).toInt() - 5) return switchTo(tabs.indexOfFirst { abs((it.y * 1000).toInt() - y) <= 5 })
        if (++tileTaps in droppedTileTaps) return
        val column = ((x - layout.firstTileX * 1000) / pitchX).roundToInt()
        val row = ((y + scrolled - restingTop).toDouble() / pitch).roundToInt()
        selected = row * layout.columns + column + skipped
    }

    /* A switch redraws the panel and brings the selected piece's row to the middle of the viewport, as far as the grid allows. */
    private fun switchTo(index: Int) {
        if (++tabTaps in droppedTabTaps) return
        if (tabTaps == leavesOnTabTap) away = true
        tab = index
        drawing = drawsTabsOver
        val place = selectedPlace() ?: return
        val middle = ((layout.grid.top + layout.grid.bottom) / 2 * 1000).toInt()
        scrolled = (restingTop + place / layout.columns * pitch - middle).coerceIn(0, floor)
    }

    /* The grid stops where its last row sits on the viewport's floor, as a list does. */
    override suspend fun drag(fromX: Int, fromY: Int, toX: Int, toY: Int, millis: Long) {
        drags++
        if (drags == leavesOnDrag) away = true
        if (drags in droppedDrags) return
        scrolled = (scrolled + (rowsPerDrag * pitch).toInt()).coerceAtMost(floor)
        gliding = (glideRows * pitch).toInt()
    }

    override fun toString() = "FakeStorage(scrolled=$scrolled, selected=$selected)"

    override suspend fun read(frame: Frame): List<Line> {
        reads++
        val fake = frame as Fake
        /* The overlay's own words, outside every region the scan reads. */
        val lines = mutableListOf(Line("Scan Stop", Box(10, 10, 120, 30)))
        if (fake.away) return lines
        if (storageOpen) lines += at("${pieces}/2,500", layout.count)
        fake.shown?.takeUnless { fake.blank }?.let { index ->
            val panel = layout.panel
            val named = if (index in unreadable) "Piece ??" else "Piece $index"
            lines += at(if (tabs.isEmpty()) named else "$named under tab ${fake.tab}", Region(panel.left, panel.top, panel.right, panel.top + (panel.bottom - panel.top) / 8))
        }
        /* Every tile in view carries its badge and its number, the way the game prints them. */
        for (row in 0 until rows) {
            val centre = rowCentre(row)
            if (centre < layout.grid.top * 1000 || centre > layout.grid.bottom * 1000) continue
            val labelTop = centre + layout.labelBelow(1000)
            val numbers = mutableListOf<Line>()
            for (column in 0 until layout.columns) {
                val index = row * layout.columns + column + skipped
                if (index >= pieces) break
                val x = layout.tileX(column, 1000)
                if (wordless) {
                    lines += Line("t25", Box(x - 30, labelTop, x + 30, labelTop + 15))
                    continue
                }
                lines += Line("+16", Box(x + 10, centre - 60, x + 40, centre - 45))
                if (index in unlabelled) continue
                numbers += Line(if (sameNumbers) "66%" else "${1000 + (index * 7) % 9}", Box(x - 30, labelTop, x + 30, labelTop + 15))
            }
            if (runsNumbersTogether && numbers.isNotEmpty()) {
                lines += Line(numbers.joinToString(" ") { it.text }, Box(numbers.first().box.left, labelTop, numbers.last().box.right, labelTop + 15))
            } else {
                lines += numbers
            }
        }
        return lines
    }

    private fun at(text: String, region: Region) = Line(text, region.box(1000, 1000))

    private companion object {
        const val TAB_X = 0.99
        /* Greys, each channel the same. */
        val DARK = Colour(40 * 0x010101)
        val FRAME = Colour(230 * 0x010101)
        /** A tile's face where tiles print no word: a saturated red. */
        val FACE = Colour(0xFFB03030.toInt())
    }
}

/** The kind a fake storage holds: a piece is the number its panel prints, so the walk is proven over no real kind. */
open class PieceScan(override val layout: GridLayout = FAKE_GRID, private val skipped: Set<Int> = emptySet()) : Scan<Int>() {
    override val serializer = Int.serializer()

    override suspend fun readTile(tapped: Tapped): Read<Int> {
        val rows = tapped.seen.rowsIn(layout.panel)
        val piece = pieceIn(rows.single())
        if (piece in skipped) return Read.Skipped

        return Read.Card(piece, rows, listOf(tapped.seen.frame), piece >= 0)
    }

    override fun readScreen(seen: Seen): Int = pieceIn(seen.rowsIn(layout.panel).single())

    override fun titleOf(rows: List<String>): String? = rows.firstOrNull()
}

/** A kind whose screen shows its grid another way than its own layout, as the heroes' squares do, told off the frame. */
class ViewedPieceScan(private val shown: GridLayout) : PieceScan() {
    override fun viewOn(seen: Seen): Scan<Int> = PieceScan(shown)
}

/** A kind whose tiles print no word, found by the colour of their face as the artifacts' are. */
class FacedPieceScan : PieceScan() {
    override fun tileAt(seen: Seen, column: Int, centreY: Int): Boolean = seen.frame.colourAt(layout.tileX(column, seen.frame.width), centreY).saturation > 0.5

    override fun firstRowCentre(seen: Seen): Int? = null
}

/** A kind whose panel is read under every tab of a fake storage, as the heroes' is, and which may end at a piece. */
class TabbedPieceScan(private val tabs: List<Spot>, private val endsAt: Int? = null) : Scan<Int>() {
    override val layout = FAKE_GRID
    override val serializer = Int.serializer()

    override suspend fun readTile(tapped: Tapped): Read<Int> {
        val shown = listOf(tapped.seen) + tabs.drop(1).map { tapped.show(it) }
        tapped.show(tabs.first())
        tapped.regrip() ?: return Read.Lost("back on the first tab, no piece is framed")
        val rows = shown.flatMap { it.rowsIn(layout.panel) }
        /* Tabs that name different pieces read as no piece. */
        val piece = rows.map(::pieceIn).distinct().singleOrNull() ?: -1
        if (piece == endsAt) return Read.Beyond

        return Read.Card(piece, rows, shown.map { it.frame }, piece >= 0)
    }

    override fun readScreen(seen: Seen): Int = pieceIn(seen.rowsIn(layout.panel).single())

    override fun titleOf(rows: List<String>): String? = rows.firstOrNull()
}

private fun pieceIn(row: String): Int = row.substringAfter("Piece ").substringBefore(" ").toIntOrNull() ?: -1

/** A grid of seven, like the game's, in round fractions that belong to no kind. */
val FAKE_GRID = GridLayout(
    columns = 7,
    firstTileX = 0.15,
    tilePitchX = 0.085,
    tilePitchY = 0.18,
    tileWidth = 0.85,
    tileHeight = 0.94,
    labelBelowCentre = 0.28,
    grid = Region(0.11, 0.19, 0.71, 0.85),
    panel = Region(0.73, 0.20, 0.98, 0.89),
    count = Region(0.60, 0.13, 0.72, 0.175),
    dragX = 0.40,
    dragFromY = 0.78,
    rowsPerDrag = 1,
)
