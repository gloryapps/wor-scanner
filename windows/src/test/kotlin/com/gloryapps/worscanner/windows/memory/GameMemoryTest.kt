package com.gloryapps.worscanner.windows.memory

import com.gloryapps.worscanner.scanner.account.readAccount
import com.gloryapps.worscanner.windows.game.Desktop
import com.sun.jna.Pointer
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.sun.jna.Memory as Buffer

class GameMemoryTest {
    private val self = ProcessHandle.current().pid().toInt()

    @BeforeTest
    fun onWindows() = assumeTrue("the desktop is Windows'", Desktop.present)

    @Test
    fun `a process's own bytes read back as they were written, from a span its memory lists`() {
        val written = Buffer(64).apply { write(0, ByteArray(64) { it.toByte() }, 0, 64) }
        val address = Pointer.nativeValue(written)

        GameMemory.ofProcess(self).getOrThrow().use { memory ->
            assertEquals((0 until 64).map { it.toByte() }, memory.read(address, 64)?.toList())
            assertTrue(memory.spans().any { address in it.start until it.start + it.size })
        }
    }

    @Test
    fun `a process Windows will not open is refused with its error`() {
        val refused = GameMemory.ofProcess(0).exceptionOrNull()

        assertTrue(refused is GameMemory.Refused && refused.code != 0, "refused with $refused")
    }

    @Test
    fun `a process holding no Lua reads as an account with nothing in it`() = runBlocking {
        val account = GameMemory.ofProcess(self).getOrThrow().use { readAccount(it, startedAt = "20261010-120000") }

        assertEquals(0, account.heroes.size + account.gear.size + account.artifacts.size)
    }
}
