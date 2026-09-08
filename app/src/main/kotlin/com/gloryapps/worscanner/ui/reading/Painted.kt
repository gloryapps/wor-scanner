package com.gloryapps.worscanner.ui.reading

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.gloryapps.worscanner.ui.Colors

/**
 * A JSON as the screen prints it: a name, what it is worth, and the punctuation that holds the two
 * together, each in its own colour. A quoted word is a name when a colon follows it.
 */
fun painted(json: String): AnnotatedString = buildAnnotatedString {
    var at = 0
    while (at < json.length) {
        val quoted = quotedAt(json, at)
        val literal = LITERALS.firstOrNull { json.startsWith(it, at) }
        when {
            quoted != null -> {
                withStyle(SpanStyle(color = if (namesSomething(json, at + quoted.length)) Colors.Json.key else Colors.Json.string)) {
                    append(quoted)
                }
                at += quoted.length
            }
            literal != null -> {
                withStyle(SpanStyle(color = Colors.Json.number)) { append(literal) }
                at += literal.length
            }
            json[at].isDigit() || (json[at] == '-' && json.getOrNull(at + 1)?.isDigit() == true) -> {
                var end = at + 1
                while (end < json.length && json[end] in DIGITS) end++
                withStyle(SpanStyle(color = Colors.Json.number)) { append(json, at, end) }
                at = end
            }
            else -> {
                withStyle(SpanStyle(color = Colors.Json.punctuation)) { append(json[at]) }
                at++
            }
        }
    }
}

/** The quoted word starting here, quotes and escapes included, or null where none starts here. */
private fun quotedAt(json: String, at: Int): String? {
    if (json[at] != '"') return null
    var end = at + 1
    while (end < json.length && json[end] != '"') end += if (json[end] == '\\') 2 else 1

    return json.substring(at, (end + 1).coerceAtMost(json.length))
}

private fun namesSomething(json: String, after: Int): Boolean {
    var at = after
    while (at < json.length && json[at].isWhitespace()) at++

    return json.getOrNull(at) == ':'
}

private val LITERALS = listOf("true", "false", "null")
private const val DIGITS = "0123456789.eE+-"
