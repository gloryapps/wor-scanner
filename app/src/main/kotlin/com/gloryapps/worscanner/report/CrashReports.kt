package com.gloryapps.worscanner.report

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.runs.Reports
import com.gloryapps.worscanner.scanner.senses.Frame

/**
 * What a release tells Crashlytics: a crash, on its own, and the scans and reads that failed
 * short of one, with the kind and the display they were reading as the keys beside the trace.
 */
class CrashReports(private val crashlytics: FirebaseCrashlytics, collecting: Boolean) : Reports {
    init {
        crashlytics.isCrashlyticsCollectionEnabled = collecting
    }

    override fun log(line: String) = crashlytics.log(line)

    override fun scanning(kind: Kind, first: Frame) {
        crashlytics.setCustomKey(KIND, kind.id)
        crashlytics.setCustomKey(DISPLAY, "${first.width}x${first.height}")
        super.scanning(kind, first)
    }

    /** A failure the app survived, sent as a non-fatal so it is counted and grouped like a crash. */
    override fun failed(what: String, cause: Throwable) {
        log("$what failed")
        crashlytics.recordException(cause)
    }

    private companion object {
        const val KIND = "kind"
        const val DISPLAY = "display"
    }
}
