package com.gloryapps.worscanner.windows.ocr

import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.scanner.text.Line

/** One word as Windows' OCR reads it, with the box it sat in. */
internal data class Word(val text: String, val box: Box)

/**
 * Windows' lines as the kinds expect them. Windows joins every word on a baseline into one line,
 * however far apart, where ML Kit splits at a wide gap; a kind tells the grid's lines from the panel's
 * by where each line sits, so a tile's word and a panel row at one height must stay two lines. A line
 * is split wherever two words stand further apart than the line is tall.
 */
internal fun linesOf(lines: List<List<Word>>): List<Line> = lines.flatMap { words ->
    val height = words.maxOfOrNull { it.box.height } ?: 0
    val runs = mutableListOf<MutableList<Word>>()
    for (word in words.sortedBy { it.box.left }) {
        val run = runs.lastOrNull()
        if (run != null && word.box.left - run.last().box.right <= height) run += word else runs += mutableListOf(word)
    }

    runs.map { run -> Line(run.joinToString(" ") { it.text }, run.map { it.box }.reduce(::union)) }
}

private fun union(a: Box, b: Box) = Box(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
