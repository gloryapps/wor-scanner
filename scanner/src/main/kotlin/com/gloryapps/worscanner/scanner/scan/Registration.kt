package com.gloryapps.worscanner.scanner.scan

import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How far the grid moved between two frames, in pixels upward, by the words printed on its tiles.
 *
 * A tile's words are read with a box; a word that was at one height and is now higher moved with
 * the grid. Words repeat across tiles, so a pair of equal words proves nothing alone, but the true
 * shift is the one every true pair agrees on and the rest scatter; pairing only within a column
 * keeps the scatter thin. A badge that starts with `+`, the same on many tiles, does not vote. The
 * tiles' art animates and cannot be trusted; the words do not move. The walk asks this only where
 * the framed tile is not found.
 *
 * The recogniser is not the same twice: it runs level numbers into one line in one frame and not
 * the next, so a line is taken apart into its words before pairing. A character it slips loses
 * that one pair and no more; forgiving the slip would pair `1056` with `1006`, two tiles. Two
 * shifts within a few pixels of each other are one, since a box's top wobbles between frames.
 *
 * Null where no shift gathers enough pairs, which is a screen that is no longer the grid.
 */
fun shiftByText(before: List<Line>, after: List<Line>, grid: Box, columnPitch: Int, rowPitch: Int): Int? {
    val was = tileWords(before, grid)
    val now = tileWords(after, grid)
    val shifts = ArrayList<Int>()

    for (word in was) {
        for (again in now) {
            if (abs(word.left - again.left) > columnPitch / 2 || word.text != again.text) continue
            shifts += word.top - again.top
        }
    }

    val needed = maxOf(FEWEST_PAIRS, was.size * SHARE / 100)
    val wobble = (rowPitch * WOBBLE).roundToInt()
    val best = shifts.map { centre -> shifts.filter { abs(it - centre) <= wobble } }
        .filter { it.size >= needed }
        .maxByOrNull { it.size } ?: return null

    return best.average().toInt()
}

/** The words on the tiles within the grid, each with where its line's box puts it. */
fun tileWords(lines: List<Line>, grid: Box): List<TileWord> = lines.filter { grid.holds(it) && it.namesATile() }.flatMap { it.tileWords() }

/** One word off a tile: what it says, in the characters that matter, and where it starts. */
data class TileWord(val text: String, val left: Int, val top: Int)

/* A line the recogniser ran together out of several tiles' numbers is spread over its box by character count. */
private fun Line.tileWords(): List<TileWord> {
    val words = ArrayList<TileWord>()
    val width = box.right - box.left
    var at = 0
    for (word in text.split(Regex("\\s+"))) {
        val start = text.indexOf(word, at)
        words += TileWord(word.lowercase().replace(NOISE, ""), box.left + width * start / text.length, box.top)
        at = start + word.length
    }

    return words
}

/** What the recogniser sprinkles around a number: anything but letters, digits and the percent sign. */
private val NOISE = Regex("[^a-z0-9%]")
/** The share of a row's pitch within which two shifts count as one. */
private const val WOBBLE = 0.045
private const val FEWEST_PAIRS = 3
/** Of the words seen before, the share that must be found again at one shift. */
private const val SHARE = 30
