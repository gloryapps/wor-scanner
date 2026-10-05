package com.gloryapps.worscanner.windows.report

import com.gloryapps.worscanner.scanner.runs.Reports
import java.io.File
import java.time.LocalDateTime

/** A log in the app's folder, where a player's report of a failure starts: Windows has no crash service here. */
internal class LogReports(private val file: File) : Reports {
    @Synchronized
    override fun log(line: String) = file.appendText("${LocalDateTime.now()} $line\n")

    override fun failed(what: String, cause: Throwable) = log("$what failed\n${cause.stackTraceToString()}")
}
