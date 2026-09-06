package com.gloryapps.worscanner.scanner.reading

/** Lower-case letters, digits and single spaces: what is left of a line once the icons' noise is gone. */
internal fun flatten(text: String): String =
    text.lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ").trim()

/** Levenshtein, so a slip of a character or two still lands a reading on its catalogue entry. */
internal fun distance(one: String, two: String): Int {
    val grid = Array(one.length + 1) { IntArray(two.length + 1) }
    for (row in 0..one.length) grid[row][0] = row
    for (column in 0..two.length) grid[0][column] = column

    for (row in 1..one.length) {
        for (column in 1..two.length) {
            val cost = if (one[row - 1] == two[column - 1]) 0 else 1
            grid[row][column] = minOf(
                grid[row - 1][column] + 1,
                grid[row][column - 1] + 1,
                grid[row - 1][column - 1] + cost,
            )
        }
    }

    return grid[one.length][two.length]
}

/** Whether a name sits anywhere in the line, with a character of slack per five of the name. */
internal fun holdsName(line: String, name: String): Boolean {
    val flat = flatten(line)
    val candidate = flatten(name)
    if (candidate.isEmpty()) return false

    val slack = candidate.length / 5
    for (at in 0..(flat.length - candidate.length + slack).coerceAtLeast(0)) {
        val end = minOf(at + candidate.length, flat.length)
        if (at >= flat.length) break
        if (distance(flat.substring(at, end), candidate) <= slack) return true
    }

    return false
}

/** The longest of the names the line holds, so `ATK` cannot shadow `ATK Bonus`. */
internal fun <T> longestHeld(line: String, candidates: Iterable<T>, nameOf: (T) -> String): T? =
    candidates.sortedByDescending { nameOf(it).length }.firstOrNull { holdsName(line, nameOf(it)) }
