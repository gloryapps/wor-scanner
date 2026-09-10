package com.gloryapps.worscanner.report

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.scanner.senses.Frame

/**
 * What a release tells Crashlytics: a crash, on its own, and the scans and reads that failed
 * short of one, with the kind and the display they were reading as the keys beside the trace.
 */
class CrashReports(private val crashlytics: FirebaseCrashlytics, collecting: Boolean) {
    init {
        crashlytics.isCrashlyticsCollectionEnabled = collecting
    }

    fun scanning(kind: Kind, first: Frame) {
        crashlytics.setCustomKey(KIND, kind.id)
        crashlytics.setCustomKey(DISPLAY, "${first.width}x${first.height}")
        crashlytics.log("scan of ${kind.id} began")
    }

    fun ended(kind: Kind, outcome: Outcome<*>) {
        when (outcome) {
            is Outcome.Finished -> crashlytics.log("scan of ${kind.id} finished, ${outcome.entries.size} read")
            is Outcome.Stopped -> crashlytics.log("scan of ${kind.id} stopped, ${outcome.reason.name.lowercase()}: ${outcome.detail}")
            is Outcome.Failed -> failed("scan of ${kind.id}", outcome.cause)
        }
    }

    /** A failure the app survived, sent as a non-fatal so it is counted and grouped like a crash. */
    fun failed(what: String, cause: Throwable) {
        crashlytics.log("$what failed")
        crashlytics.recordException(cause)
    }

    private companion object {
        const val KIND = "kind"
        const val DISPLAY = "display"
    }
}
