package com.gloryapps.worscanner.ui

import android.content.Context
import android.text.format.Formatter
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Outbound
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The stamp as a person reads it, in their own locale: `6 Sept 2026 · 19:18`. */
fun Kept.shown(): String = "${at.format(DAY)} · ${at.format(HOUR)}"

/** The line under the stamp: how much was read, how big it is on disk, and how it ended. */
fun Kept.said(context: Context): String = listOfNotNull(
    entries?.let { "$it ${kind?.let { each -> context.getString(each.label) }.orEmpty()}".trim() }
        ?: context.getString(R.string.home_reading),
    Formatter.formatShortFileSize(context, weight()),
    outcome?.let { ended(context) },
).joinToString(" · ")

/** One reading on its way out: the JSON it goes out under, and the files that travel with it. */
fun Kept.outgoing(context: Context): Outgoing {
    val leaving = outbound()

    return Outgoing(
        name = outgoingName(),
        files = leaving,
        holds = buildList {
            kind?.let { add(context.getString(R.string.export_kind) to context.getString(it.label)) }
            entries?.let { add(context.getString(R.string.export_entries) to "$it") }
            outcome?.let { add(context.getString(R.string.export_outcome) to ended(context)) }
            add(context.getString(R.string.export_size) to Formatter.formatShortFileSize(context, weight()))
        },
    )
}

/** The name the JSON leaves under, which is what the reading screen calls the file. */
fun Kept.outgoingName(): String = outbound().firstOrNull { it.file.extension == "json" }?.name ?: exportName()

/** Every reading at once, which is what `export all` sends. */
fun List<Kept>.outgoing(context: Context): Outgoing = Outgoing(
    name = context.resources.getQuantityString(R.plurals.export_all_name, size, size),
    files = flatMap { it.outbound() },
    holds = listOf(
        context.getString(R.string.export_readings) to "$size",
        context.getString(R.string.export_size) to Formatter.formatShortFileSize(context, sumOf { it.weight() }),
    ),
)

/**
 * The names the files leave under, which is the export's whole shape: `wor-gear-20260907-130812.json`,
 * and a scan's kept panels beside it under the tile each was read from. The stamp keeps two exports apart.
 */
fun Kept.outbound(): List<Outbound> {
    val export = exportName()

    return files.map { file ->
        Outbound(
            file,
            when {
                file.extension == "json" -> "$export.json"
                form == "reading" -> "$export.png"
                else -> "$export-${file.nameWithoutExtension}.png"
            },
        )
    }
}

private fun Kept.exportName(): String = listOfNotNull("wor", kind?.id, stamp).joinToString("-")

/** How a scan ended, in one word. Why it ended that way is `detail`, which is a sentence. */
fun Kept.ended(context: Context): String = when {
    outcome == null -> context.getString(R.string.home_reading)
    outcome == "finished" -> context.getString(R.string.home_complete)
    outcome.startsWith("stopped") -> context.getString(R.string.home_stopped)
    else -> context.getString(R.string.home_failed)
}

private fun Kept.weight(): Long = files.sumOf { it.length() }

private val DAY: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val HOUR: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
