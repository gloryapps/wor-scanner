package com.gloryapps.worscanner.scanner.text

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

/**
 * Whether a name sits anywhere in the line, with a character of slack per five of the name. The
 * window tried is as long as the name give or take the slack, since `Bornus` for `Bonus` is one
 * character more, not one different.
 */
internal fun holdsName(line: String, name: String): Boolean {
    val flat = flatten(line)
    val candidate = flatten(name)
    if (candidate.isEmpty()) return false

    val slack = candidate.length / 5
    for (at in 0 until flat.length) {
        for (length in (candidate.length - slack)..(candidate.length + slack)) {
            val end = at + length
            if (length <= 0 || end > flat.length) continue
            if (distance(flat.substring(at, end), candidate) <= slack) return true
        }
    }

    return false
}

/**
 * How many times a word sits in the line, letters only, with a character of slack per four of the
 * word: `Mextevel` and `Max Letel` are still `Max Level`. Each time is the closest window to the
 * word, and the next is looked for after it.
 */
internal fun timesHeld(line: String, word: String): Int {
    val flat = line.lowercase().filter { it in 'a'..'z' }
    val target = word.lowercase().filter { it in 'a'..'z' }
    if (target.isEmpty()) return 0
    val slack = target.length / 4
    var times = 0
    var at = 0
    while (at < flat.length) {
        val held = ((target.length - slack)..(target.length + slack))
            .filter { at + it <= flat.length }
            .map { length -> length to distance(flat.substring(at, at + length), target) }
            .filter { (_, off) -> off <= slack }
            .minByOrNull { (_, off) -> off }
        if (held == null) {
            at++
        } else {
            times++
            at += held.first
        }
    }

    return times
}

/** Whether a word is this one read again: the same, or off by one character where there are enough to be sure. */
internal fun readsAs(word: String, name: String): Boolean =
    word.equals(name, ignoreCase = true) || name.length >= SURE_LENGTH && distance(word.lowercase(), name.lowercase()) <= 1

/** Letters a word needs before a slip of one is forgiven: `Ring` and `King` are too short to tell apart. */
private const val SURE_LENGTH = 5

/** Which candidate's name the line holds behind the icons' noise, the longest where several, so `ATK` cannot shadow `ATK Bonus`. */
fun <T> nameIn(line: String, candidates: Iterable<T>, nameOf: (T) -> String): T? =
    candidates.sortedByDescending { nameOf(it).length }.firstOrNull { holdsName(line, nameOf(it)) }
