package com.gloryapps.worscanner.scanner.azhor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

/** Where the token is kept between runs. */
interface TokenStore {
    val token: Flow<String?>

    /** The token kept from now on; null forgets it. */
    suspend fun keep(token: String?)
}

/** The scanner's link to the site: the token a code was traded for, and the scans sent with it. */
class Link(private val api: AzhorApi, private val store: TokenStore) {
    val linked: Flow<Boolean> = store.token.map { it != null }

    suspend fun link(code: String): Linking = api.link(code).also { if (it is Linking.Linked) store.keep(it.token) }

    suspend fun forget() = store.keep(null)

    /** Each scan sent in turn, stopping at the first the site did not take; a token the site no longer knows is forgotten. */
    suspend fun send(scans: List<File>): Sending {
        val token = store.token.first() ?: return Sending.Unlinked
        for (scan in scans) {
            val sent = api.send(token, scan)
            if (sent == Sending.Unlinked) forget()
            if (sent != Sending.Sent) return sent
        }

        return Sending.Sent
    }
}
