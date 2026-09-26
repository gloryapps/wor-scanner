package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.resultOf
import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.senses.Screen
import com.gloryapps.worscanner.scanner.senses.TextReader
import com.gloryapps.worscanner.scanner.senses.Touch
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import com.gloryapps.worscanner.scanner.text.rowsOf
import kotlinx.serialization.KSerializer

/**
 * How one kind is scanned: where its screen puts things, how its record is written, and what it
 * reads off each tile. The walk over the grid is every kind's and runs in [run]; a kind says only
 * what a tile holds.
 */
abstract class Scan<T> {
    abstract val layout: GridLayout
    abstract val serializer: KSerializer<T>

    /** Reads the tile the walk just tapped, off the frame the tap left and whatever else the walk lends. */
    abstract suspend fun readTile(tapped: Tapped): Read<T>

    /** What the kind makes of a whole frame, as the overlay's Read keeps it. */
    abstract fun readScreen(seen: Seen): T

    /** Whether a tile sits at this column and row centre, the grid ending where none does; by default, by the word it prints below its centre. */
    open fun tileAt(seen: Seen, column: Int, centreY: Int): Boolean = labelledTileAt(seen.lines, layout, seen.frame, column, centreY)

    /**
     * Scans the grid from where it stands to its end, filling `entries` as it goes.
     *
     * The list is the caller's so that a cancelled scan, which ends by the exception it must, still
     * leaves the caller holding what was read before it.
     */
    suspend fun run(
        screen: Screen,
        touch: Touch,
        reader: TextReader,
        keeper: Keeper,
        entries: MutableList<ScanEntry<T>>,
        settleMillis: Long = 250,
        progress: suspend (Progress) -> Unit = {},
    ): Outcome<T> =
        resultOf { Walk(this, screen, touch, reader, keeper, settleMillis, entries, progress).run() }.getOrElse { Outcome.Failed(it, entries) }
}

/** What a frame showed: the picture, and every line the recogniser read off it. */
class Seen(val frame: Frame, val lines: List<Line>) {
    /** The rows the frame prints inside the region. */
    fun rowsIn(region: Region): List<String> {
        val box = region.box(frame.width, frame.height)

        return rowsOf(lines.filter { box.holds(it) })
    }
}

/** The tile the walk just tapped, as its kind reads it: the frame the tap left, and what the walk lends for more. */
interface Tapped {
    val seen: Seen

    /** The tapped tile's rectangle on the frame the tap left. */
    val tile: Box

    /** Taps a tab until the panel shows something else, a tap the game dropped tapped again; the frame it shows it on. */
    suspend fun show(tab: Spot): Seen

    /** Once the kind's taps have moved the grid, finds the tile again by its frame and takes the grid's place from it; null where it is gone. */
    suspend fun regrip(): Seen?
}

/** What a kind made of the tile the walk just tapped. */
sealed interface Read<out T> {
    /** The record, the rows it was read from, the frames that show them, and whether it names what identifies it. */
    class Card<T>(val card: T, val rows: List<String>, val frames: List<Frame>, val closed: Boolean) : Read<T>

    /** The tile is past what the kind scans: the scan finishes before it. */
    data object Beyond : Read<Nothing>

    /** The kind lost the grid reading the tile: the scan stops there. */
    class Lost(val detail: String) : Read<Nothing>
}

/** Where the panel of a tile the kind did not close goes, every frame's in one image under the entry's index, so the image can answer what the text could not. */
fun interface Keeper {
    suspend fun keep(frames: List<Frame>, index: Int): String
}
