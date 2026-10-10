package com.gloryapps.worscanner.scanner.azhor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SenderTest {
    /** A lab that links one code and answers every scan as it is told to. */
    private class Lab(private val answer: Sending = Sending.Sent) : AzhorApi {
        override suspend fun link(code: String): Linking = if (code == CODE) Linking.Linked("token") else Linking.Refused

        override suspend fun send(token: String, scan: File): Sending = answer
    }

    private class Kept(token: String?) : TokenStore {
        override val token = MutableStateFlow(token)

        override suspend fun keep(token: String?) {
            this.token.value = token
        }
    }

    private val scans = listOf(File("scan.json"))

    private fun sender(token: String? = null, answer: Sending = Sending.Sent) = Sender(Link(Lab(answer), Kept(token)))

    private fun shown(block: suspend (show: (Send) -> Unit) -> Unit): List<Send> = mutableListOf<Send>().also { shown -> runBlocking { block { shown += it } } }

    @Test
    fun `a scanner not linked starts on the link step, a linked one on nothing tried`() = runBlocking {
        assertEquals(Send.Code(), sender().start())
        assertEquals(Send.Idle, sender(token = "token").start())
    }

    @Test
    fun `sending unlinked asks for the code`() {
        assertEquals(listOf<Send>(Send.Code()), shown { sender().send(scans, it) })
    }

    @Test
    fun `a code the lab links sends the scan straight after`() {
        assertEquals(listOf(Send.Underway, Send.Underway, Send.Sent), shown { sender().linkAndSend(CODE, scans, it) })
    }

    @Test
    fun `a code the lab links with nothing to send leaves the scanner linked, Send one press away`() {
        assertEquals(listOf(Send.Underway, Send.Idle), shown { sender().link(CODE, it) })
    }

    @Test
    fun `a code the lab refuses with nothing to send goes back to the link step, saying so`() {
        assertEquals(listOf(Send.Underway, Send.Code(failed = Linking.Refused)), shown { sender().link("WRONG", it) })
    }

    @Test
    fun `an unlinked scanner says so, and starts on the link step`() = runBlocking {
        val sender = sender(token = "token")

        sender.unlink()

        assertEquals(false, sender.linked.first())
        assertEquals(Send.Code(), sender.start())
    }

    @Test
    fun `a code the lab refuses goes back to the link step, saying so`() {
        assertEquals(listOf(Send.Underway, Send.Code(failed = Linking.Refused)), shown { sender().linkAndSend("WRONG", scans, it) })
    }

    @Test
    fun `a scan the lab does not take says why`() {
        assertEquals(listOf(Send.Underway, Send.Unsent(Sending.TooLarge)), shown { sender(token = "token", answer = Sending.TooLarge).send(scans, it) })
    }

    private companion object {
        const val CODE = "ABCDEF"
    }
}
