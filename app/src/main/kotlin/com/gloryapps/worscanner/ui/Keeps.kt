package com.gloryapps.worscanner.ui

import android.content.Context
import android.text.format.Formatter
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.capture.Kept
import com.gloryapps.worscanner.capture.Outbound
import com.gloryapps.worscanner.scan.Ended
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The stamp as a person reads it, in their own locale: `6 Sept 2026 · 19:18`. */
fun Kept.shown(): String = "${at.format(DAY)} · ${at.format(HOUR)}"

/** The line under the stamp: what was read and how much, how big it is on disk, and how it ended. */
fun Kept.said(context: Context): String = listOfNotNull(
    kind?.let { context.getString(it.label) },
    when (this) {
        is Kept.Read -> context.getString(R.string.home_reading)
        is Kept.Scan -> entries?.let { context.getString(R.string.overlay_kept, it) }
    },
    Formatter.formatShortFileSize(context, weight()),
    (this as? Kept.Scan)?.ended(context),
).joinToString(" · ")

/** One reading on its way out: the JSON it goes out under, and the files that travel with it. */
fun Kept.outgoing(context: Context): Outgoing {
    val leaving = outbound()

    return Outgoing(
        name = outgoingName(),
        files = leaving,
        scans = scans(),
        holds = buildList {
            kind?.let { add(context.getString(R.string.export_kind) to context.getString(it.label)) }
            if (this@outgoing is Kept.Scan) {
                entries?.let { add(context.getString(R.string.export_entries) to "$it") }
                add(context.getString(R.string.export_outcome) to ended(context))
            }
            images(context)?.let(::add)
            add(context.getString(R.string.export_size) to Formatter.formatShortFileSize(context, weight()))
        },
    )
}

/** The JSON of a scan, which is what the lab imports; a Read's is not one. */
internal fun Kept.scans(): List<File> = if (this is Kept.Scan) files.filter { it.extension == "json" } else emptyList()

/** Why images travel with the JSON, said in the sheet so nobody wonders what the PNGs beside it are. */
private fun Kept.images(context: Context): Pair<String, String>? {
    val panels = files.count { it.extension == "png" }

    return when {
        panels == 0 -> null
        this is Kept.Read -> context.getString(R.string.export_frame) to context.getString(R.string.export_frame_read)
        this is Kept.Scan && entries == 0 -> context.getString(R.string.export_frame) to context.getString(R.string.export_frame_first)
        else -> context.getString(R.string.export_panels) to context.resources.getQuantityString(R.plurals.export_open, panels, panels)
    }
}

/** The name the JSON leaves under, which is what the reading screen calls the file. */
fun Kept.outgoingName(): String = outbound().firstOrNull { it.file.extension == "json" }?.name ?: exportName()

/** Every reading at once, which is what `export all` sends. */
fun List<Kept>.outgoing(context: Context): Outgoing = Outgoing(
    name = context.resources.getQuantityString(R.plurals.export_all_name, size, size),
    files = flatMap { it.outbound() },
    scans = flatMap { it.scans() },
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
                this is Kept.Read -> "$export.png"
                else -> "$export-${file.nameWithoutExtension}.png"
            },
        )
    }
}

private fun Kept.exportName(): String = listOfNotNull("wor", kind?.id, stamp).joinToString("-")

/** How a scan ended, in one word. Why it ended that way is `detail`, which is a sentence. */
fun Kept.Scan.ended(context: Context): String = context.getString(
    when (ended) {
        Ended.FINISHED -> R.string.home_complete
        Ended.STOPPED -> R.string.home_stopped
        Ended.FAILED, null -> R.string.home_failed
    },
)

private fun Kept.weight(): Long = files.sumOf { it.length() }

private val DAY: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val HOUR: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
