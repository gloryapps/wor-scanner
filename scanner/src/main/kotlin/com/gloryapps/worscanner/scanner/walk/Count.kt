package com.gloryapps.worscanner.scanner.walk

/** The pieces held, off the header's `1,169/2,500`. */
fun countIn(rows: List<String>): Int? {
    val found = rows.firstNotNullOfOrNull { Regex("(\\d[\\d,.]*)\\s*/\\s*\\d[\\d,.]*").find(it) } ?: return null

    return found.groupValues[1].replace(Regex("[,.]"), "").toIntOrNull()
}
