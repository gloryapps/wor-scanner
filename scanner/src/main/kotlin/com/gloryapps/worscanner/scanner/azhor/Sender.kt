package com.gloryapps.worscanner.scanner.azhor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

/** Where sending a scan to the lab stands, as every platform shows it. */
sealed interface Send {
    /** Nothing tried yet. */
    data object Idle : Send

    /** The link step, the code the lab shows typed here; `failed` is why the last code did not link. */
    data class Code(val failed: Linking? = null) : Send

    data object Underway : Send

    data object Sent : Send

    /** The lab did not take it, and said why: unlinked, too large, refused, or no answer. */
    data class Unsent(val why: Sending) : Send
}

/** A scan sent to the lab, linking the scanner first where it is not: each step handed to `show` as it stands. */
class Sender(private val link: Link) {
    val linked: Flow<Boolean> = link.linked

    /** Where sending starts: the link step while the scanner is not linked. */
    suspend fun start(): Send = if (link.linked.first()) Send.Idle else Send.Code()

    suspend fun send(scans: List<File>, show: (Send) -> Unit) = if (link.linked.first()) deliver(scans, show) else show(Send.Code())

    suspend fun linkAndSend(code: String, scans: List<File>, show: (Send) -> Unit) {
        if (linked(code, show)) deliver(scans, show)
    }

    /** The scanner linked with nothing to send yet: once it is, Send is one press. */
    suspend fun link(code: String, show: (Send) -> Unit) {
        if (linked(code, show)) show(Send.Idle)
    }

    suspend fun unlink() = link.forget()

    /** Whether the code linked; where it did not, the link step again, saying why. */
    private suspend fun linked(code: String, show: (Send) -> Unit): Boolean {
        show(Send.Underway)

        return when (val linking = link.link(code)) {
            is Linking.Linked -> true
            Linking.Refused, Linking.Unanswered -> false.also { show(Send.Code(failed = linking)) }
        }
    }

    private suspend fun deliver(scans: List<File>, show: (Send) -> Unit) {
        show(Send.Underway)
        val sent = link.send(scans)
        show(if (sent == Sending.Sent) Send.Sent else Send.Unsent(sent))
    }
}
