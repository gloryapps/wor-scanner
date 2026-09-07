package com.gloryapps.worscanner.scanner.text

/**
 * Which candidate the row holds as a whole word, a character off at most: a loose match once read
 * `Amulet` as `Ring`, and `Amnulet` and `Banglet` are what the recogniser makes of the real words.
 */
fun <T> wordIn(row: String, candidates: Iterable<T>, wordOf: (T) -> String): T? {
    val words = row.split(Regex("[^A-Za-z]+")).filter { it.isNotEmpty() }

    return candidates.firstOrNull { candidate -> words.any { readsAs(it, wordOf(candidate)) } }
}
