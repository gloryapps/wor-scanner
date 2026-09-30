package com.gloryapps.worscanner.smithy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkTest {
    /** A site that trades one code for one token, and answers each scan sent as it is told to. */
    private class Site(private val answers: List<Sending> = emptyList()) : Smithy {
        val sent = mutableListOf<Pair<String, File>>()

        override suspend fun link(code: String): Linking = if (code == CODE) Linking.Linked(TOKEN) else Linking.Refused

        override suspend fun send(token: String, scan: File): Sending {
            sent += token to scan

            return answers.getOrElse(sent.size - 1) { Sending.Sent }
        }
    }

    private class Kept(token: String? = null) : TokenStore {
        override val token = MutableStateFlow(token)

        override suspend fun keep(token: String?) {
            this.token.value = token
        }
    }

    private val scans = listOf(File("wor-gear.json"), File("wor-heroes.json"), File("wor-artifacts.json"))

    @Test
    fun `a code the site trades leaves the scanner linked with its token`() = runBlocking {
        val kept = Kept()
        val link = Link(Site(), kept)

        assertEquals(Linking.Linked(TOKEN), link.link(CODE))
        assertEquals(TOKEN, kept.token.value)
        assertTrue(link.linked.first())
    }

    @Test
    fun `a code the site refuses keeps nothing`() = runBlocking {
        val link = Link(Site(), Kept())

        assertEquals(Linking.Refused, link.link("0000-0000"))
        assertFalse(link.linked.first())
    }

    @Test
    fun `every scan is sent with the kept token`() = runBlocking {
        val site = Site()

        assertEquals(Sending.Sent, Link(site, Kept(TOKEN)).send(scans))
        assertEquals(scans.map { TOKEN to it }, site.sent)
    }

    @Test
    fun `the first scan the site does not take stops the rest`() = runBlocking {
        val site = Site(listOf(Sending.Sent, Sending.TooLarge))

        assertEquals(Sending.TooLarge, Link(site, Kept(TOKEN)).send(scans))
        assertEquals(2, site.sent.size)
    }

    @Test
    fun `a token the site no longer knows is forgotten`() = runBlocking {
        val kept = Kept(TOKEN)

        assertEquals(Sending.Unlinked, Link(Site(listOf(Sending.Unlinked)), kept).send(scans))
        assertNull(kept.token.value)
    }

    @Test
    fun `nothing is sent by a scanner never linked`() = runBlocking {
        val site = Site()

        assertEquals(Sending.Unlinked, Link(site, Kept()).send(scans))
        assertTrue(site.sent.isEmpty())
    }

    @Test
    fun `the site's answer to a code is a token, a refusal, or nothing learnt`() {
        assertEquals(Linking.Linked(TOKEN), linkingOf(200, """{"token": "$TOKEN"}"""))
        assertEquals(Linking.Refused, linkingOf(404, null))
        assertEquals(Linking.Refused, linkingOf(400, null))
        assertEquals(Linking.Unanswered, linkingOf(200, "<html>"))
        assertEquals(Linking.Unanswered, linkingOf(500, null))
    }

    @Test
    fun `the site's answer to a scan is read by its status`() {
        assertEquals(Sending.Sent, sendingOf(204))
        assertEquals(Sending.Unlinked, sendingOf(401))
        assertEquals(Sending.Refused, sendingOf(400))
        assertEquals(Sending.TooLarge, sendingOf(413))
        assertEquals(Sending.Unanswered, sendingOf(502))
    }

    private companion object {
        const val CODE = "K7Q4-MPX2"
        const val TOKEN = "a-token"
    }
}
