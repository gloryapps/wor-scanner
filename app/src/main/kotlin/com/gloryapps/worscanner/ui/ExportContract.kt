package com.gloryapps.worscanner.ui

import android.content.Intent
import com.gloryapps.worscanner.capture.Outbound
import com.gloryapps.worscanner.capture.SharedFolder
import java.io.File

/** What one press of export puts on its way out, each file under the name it lands by, what the sheet says it holds, and the scans' JSON among it that the lab imports. */
data class Outgoing(val name: String, val files: List<Outbound>, val holds: List<Pair<String, String>>, val scans: List<File> = emptyList()) {
    val forLab: Boolean get() = scans.isNotEmpty()
}

/** The export sheet: what is on its way out, the folders it can land in and the one chosen, and why the last save did not land. */
data class ExportUiState(
    val outgoing: Outgoing? = null,
    val shared: List<SharedFolder> = emptyList(),
    val into: SharedFolder? = null,
    val failed: String? = null,
    /** Whether the scanner holds a token from the site, which is what lets a scan be sent there. */
    val linked: Boolean = false,
    val sending: Boolean = false,
)

sealed interface ExportEvent {

    data class Choose(val shared: SharedFolder) : ExportEvent

    data object Save : ExportEvent

    data object Share : ExportEvent

    /** The scans sent to Azhor's Master Smithy, where the account's Import a scan holds them. */
    data object Send : ExportEvent

    data object Close : ExportEvent
}

sealed interface ExportEffect {

    /** The other app's chooser, over the files staged under their outbound names. */
    data class Share(val intent: Intent) : ExportEffect

    data class Landed(val folder: String, val files: Int) : ExportEffect

    data class Sent(val scans: Int) : ExportEffect
}
