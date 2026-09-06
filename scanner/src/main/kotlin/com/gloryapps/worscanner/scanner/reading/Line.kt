package com.gloryapps.worscanner.scanner.reading

import kotlinx.serialization.Serializable

/** Where a line sits in the frame it was read from, in that frame's pixels. */
@Serializable
data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val height: Int get() = bottom - top
    val middle: Int get() = (top + bottom) / 2
}

/** One line of text as the recogniser gave it up, with the box it sat in. */
@Serializable
data class Line(val text: String, val box: Box)

/**
 * The lines gathered into the rows the card prints, top to bottom and left to right within a row.
 *
 * A row's name and its value sit far apart on the panel and come back as two lines; joined by
 * where they sit rather than by the order the recogniser chose, `ATK Bonus` and `66%` are one row.
 */
fun rowsOf(lines: List<Line>): List<String> {
    val sorted = lines.sortedBy { it.box.middle }
    val rows = mutableListOf<MutableList<Line>>()

    for (line in sorted) {
        val row = rows.lastOrNull()
        if (row != null && line.box.middle - row.first().box.middle < row.first().box.height / 2) {
            row += line
        } else {
            rows += mutableListOf(line)
        }
    }

    return rows.map { row -> row.sortedBy { it.box.left }.joinToString(" ") { it.text.trim() } }
}
