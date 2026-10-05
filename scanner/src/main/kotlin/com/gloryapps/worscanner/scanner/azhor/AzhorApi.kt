package com.gloryapps.worscanner.scanner.azhor

import java.io.File

/** Azhor's Master Smithy, as a scanner reaches it: a code traded once for a token, and scans sent with that token. */
interface AzhorApi {
    /** What the site made of a code the player typed. */
    suspend fun link(code: String): Linking

    /** What the site made of a scan file sent with the token. */
    suspend fun send(token: String, scan: File): Sending
}

sealed interface Linking {
    data class Linked(val token: String) : Linking

    /** No account waits on that code: mistyped, already traded, or past its ten minutes. */
    data object Refused : Linking

    /** The site could not be reached, or did not answer as it says it will. */
    data object Unanswered : Linking
}

sealed interface Sending {
    data object Sent : Sending

    /** The site no longer knows the token: the account unlinked the scanner, or linked another. */
    data object Unlinked : Sending

    /** The site read the file as no scan. */
    data object Refused : Sending

    /** The scan is larger than the site keeps. */
    data object TooLarge : Sending

    data object Unanswered : Sending
}
