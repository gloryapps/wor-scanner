package com.gloryapps.worscanner.ui

import com.gloryapps.worscanner.scanner.runs.Kept
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The stamp as a person reads it, in their own locale: `6 Sept 2026 · 19:18`. */
fun Kept.shown(): String = "${at.format(DAY)} · ${at.format(HOUR)}"

private val DAY: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val HOUR: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
