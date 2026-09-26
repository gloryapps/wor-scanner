package com.gloryapps.worscanner.scanner.game

import com.gloryapps.worscanner.scanner.text.holdsName

/**
 * What a panel names above the word `Exclusive`, as read; who that is belongs to the lab.
 *
 * The word sits on its own row under the name; the icon leaves single letters in front of it.
 */
internal fun exclusiveIn(rows: List<String>): String? {
    val head = headOf(rows)
    val at = head.indexOfFirst { holdsName(it, "Exclusive") }
    if (at < 0) return null

    val named = "${head[at].replace(Regex("exclusive", RegexOption.IGNORE_CASE), "")} ${head.getOrNull(at - 1) ?: ""}"
        .replace(Regex("[^A-Za-z' ]"), " ")
        .split(Regex("\\s+"))
        .filter { it.length > 1 }
        .joinToString(" ")
        .trim()

    return named.ifEmpty { null }
}
