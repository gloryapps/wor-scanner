package com.gloryapps.worscanner.scanner.walk

import com.gloryapps.worscanner.scanner.reading.Box
import com.gloryapps.worscanner.scanner.reading.Line
import kotlin.math.abs

/**
 * How far the grid moved between two frames, in pixels upward, by the words printed on its tiles.
 *
 * Every tile carries its enhancement badge and its main number, which the recogniser reads with a
 * box; a number that was at one height and is now higher moved with the grid. Numbers repeat across
 * tiles, so a pair of equal words proves nothing alone, but the true shift is the one every true
 * pair agrees on and the rest scatter; pairing only within a column keeps the scatter thin. The
 * badge reads `+16` on every tile and says nothing about which, so it does not vote. A line the
 * recogniser ran together out of several tiles' numbers votes once per number, since it stands for
 * that many tiles. The tiles' art animates and cannot be trusted; the words do not move.
 *
 * Null where no shift gathers enough pairs, which is a screen that is no longer the storage.
 */
fun shiftByText(before: List<Line>, after: List<Line>, grid: Box, columnPitch: Int): Int? {
    val was = before.filter { grid.holds(it) && it.namesATile() }
    val now = after.filter { grid.holds(it) && it.namesATile() }.groupBy { it.text }
    val votes = HashMap<Int, MutableList<Int>>()

    for (line in was) {
        for (again in now[line.text].orEmpty()) {
            if (abs(line.box.left - again.box.left) > columnPitch / 2) continue
            val shift = line.box.top - again.box.top
            repeat(line.words()) { votes.getOrPut(Math.floorDiv(shift, BUCKET)) { mutableListOf() } += shift }
        }
    }

    val needed = maxOf(FEWEST_PAIRS, was.sumOf { it.words() } * SHARE / 100)
    val best = votes.values.filter { it.size >= needed }.maxByOrNull { it.size } ?: return null

    return best.average().toInt()
}

private fun Line.words(): Int = text.trim().split(Regex("\\s+")).size

/** Pixels within which two shifts count as one vote. */
private const val BUCKET = 6
private const val FEWEST_PAIRS = 3
/** Of the words seen before, the share that must be found again at one shift. */
private const val SHARE = 30
