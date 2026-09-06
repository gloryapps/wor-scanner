package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.Frame
import kotlin.math.abs

/** A tile's art boiled down to a few brightness samples, enough to tell whether two tiles are one. */
class Fingerprint(private val samples: IntArray) {
    fun distance(other: Fingerprint): Int = samples.indices.sumOf { abs(samples[it] - other.samples[it]) } / samples.size

    companion object {
        private const val GRID = 4

        fun of(frame: Frame, centreX: Int, centreY: Int, half: Int): Fingerprint {
            val samples = IntArray(GRID * GRID)
            for (row in 0 until GRID) {
                for (column in 0 until GRID) {
                    val x = centreX - half + (2 * column + 1) * half / GRID
                    val y = centreY - half + (2 * row + 1) * half / GRID
                    samples[row * GRID + column] = frame.luminanceAt(x.coerceIn(0, frame.width - 1), y.coerceIn(0, frame.height - 1))
                }
            }

            return Fingerprint(samples)
        }
    }
}

/** A row of tiles at one height of a frame, for finding it again on the next. */
class RowPrint(private val tiles: List<Fingerprint>) {
    fun distance(other: RowPrint): Int = tiles.zip(other.tiles).maxOf { (one, two) -> one.distance(two) }

    companion object {
        fun of(frame: Frame, layout: StorageLayout, centreY: Int): RowPrint {
            val half = (layout.tilePitchX * frame.width * layout.tileSample).toInt()

            return RowPrint((0 until layout.columns).map { column -> Fingerprint.of(frame, layout.tileX(column, frame.width), centreY, half) })
        }
    }
}

/**
 * How far the grid moved between two frames, in pixels upward: the shift under which the rows
 * seen before are found again, tile for tile.
 *
 * Null where no shift makes them meet, which is a scroll that went further than a screen or a
 * screen that is no longer the storage. A drag need not stop on a row, so the shift is any pixel.
 */
fun shiftBetween(before: Frame, after: Frame, layout: StorageLayout, rowCentres: List<Int>, upTo: Int): Int? {
    /* Only a tile whose samples all sit inside the viewport reads the same on both frames. */
    val half = (layout.tilePitchX * after.width * layout.tileSample).toInt()
    val top = (layout.gridTop * after.height).toInt() + half
    val bottom = (layout.gridBottom * after.height).toInt() - half
    val whole = rowCentres.filter { it in top..bottom }
    val prints = whole.map { RowPrint.of(before, layout, it) }
    var best: Pair<Int, Int>? = null

    for (shift in 0..upTo step STEP) {
        val landed = whole.indices.filter { whole[it] - shift in top..bottom }
        if (landed.isEmpty()) continue
        val distance = landed.maxOf { prints[it].distance(RowPrint.of(after, layout, whole[it] - shift)) }
        if (distance <= ALIKE && (best == null || distance < best.second)) best = shift to distance
    }

    return best?.first
}

/** Mean brightness difference per sample under which two tiles are taken for the same art. */
const val ALIKE = 12

private const val STEP = 2
