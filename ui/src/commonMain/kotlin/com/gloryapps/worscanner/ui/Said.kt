package com.gloryapps.worscanner.ui

import com.gloryapps.worscanner.scanner.runs.ReadScreen
import com.gloryapps.worscanner.scanner.runs.ScanState
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.ui.resources.Res
import com.gloryapps.worscanner.ui.resources.read_failed
import com.gloryapps.worscanner.ui.resources.read_kept
import com.gloryapps.worscanner.ui.resources.scan_failed
import com.gloryapps.worscanner.ui.resources.scan_finished
import com.gloryapps.worscanner.ui.resources.scan_running
import com.gloryapps.worscanner.ui.resources.scan_stopped
import org.jetbrains.compose.resources.getString

/** What a Read came to, as every platform tells the player: the stamp it was kept under and how many lines it read, or why it failed. */
suspend fun Result<ReadScreen.Read>.said(): String = fold(
    onSuccess = { getString(Res.string.read_kept, it.kept.stamp, it.lines) },
    onFailure = { getString(Res.string.read_failed, it.message ?: it.toString()) },
)

/** A scan under way or ended, as every platform tells the player; null while none has begun. */
suspend fun ScanState.said(): String? = when (this) {
    ScanState.Idle -> null
    is ScanState.Running -> getString(Res.string.scan_running, getString(kind.label), progress.done, progress.held)
    is ScanState.Ended -> when (val outcome = outcome) {
        is Outcome.Finished<*> -> getString(Res.string.scan_finished, getString(kind.label), outcome.entries.size)
        is Outcome.Stopped<*> -> getString(Res.string.scan_stopped, getString(kind.label), outcome.detail, outcome.entries.size)
        is Outcome.Failed<*> -> getString(Res.string.scan_failed, getString(kind.label), outcome.cause.toString())
    }
}
