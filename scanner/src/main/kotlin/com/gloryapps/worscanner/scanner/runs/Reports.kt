package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.senses.Frame

/**
 * Where the platform tells how scans and reads went beyond their files: a scan's start and end, said
 * here the same for every platform as lines, and a failure the app survived.
 */
interface Reports {
    /** One line of what happened, kept wherever the platform keeps them. */
    fun log(line: String)

    fun failed(what: String, cause: Throwable)

    fun scanning(kind: Kind, first: Frame) = log("scan of ${kind.id} began on ${first.width}x${first.height}")

    fun ended(kind: Kind, outcome: Outcome<*>) = when (outcome) {
        is Outcome.Finished -> log("scan of ${kind.id} finished, ${outcome.entries.size} read")
        is Outcome.Stopped -> log("scan of ${kind.id} stopped, ${outcome.reason.name.lowercase()}: ${outcome.detail}")
        is Outcome.Failed -> failed("scan of ${kind.id}", outcome.cause)
    }
}
