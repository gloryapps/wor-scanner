package com.gloryapps.worscanner.windows

import com.gloryapps.worscanner.windows.game.Desktop
import com.gloryapps.worscanner.windows.game.TopWindow
import com.gloryapps.worscanner.windows.memory.GameMemory
import com.sun.jna.Pointer
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.sun.jna.Memory as Buffer

/** A window titled as the game on a real Windows desktop, found and its process read. Run on a Windows runner by hand; skipped on any other system. */
class OnTheDesktopTest {
    private lateinit var standIn: JFrame

    @BeforeTest
    fun open() {
        assumeTrue("the desktop is Windows'", Desktop.present)
        SwingUtilities.invokeAndWait {
            standIn = JFrame("Watcher of Realms").apply {
                contentPane = JPanel().apply { preferredSize = Dimension(WIDTH, HEIGHT) }
                pack()
                setLocation(80, 80)
                isVisible = true
            }
        }
        Thread.sleep(SETTLE_MS)
    }

    @AfterTest
    fun close() {
        if (::standIn.isInitialized) SwingUtilities.invokeAndWait { standIn.dispose() }
    }

    private fun game(): TopWindow = assertNotNull(runBlocking { Desktop.game() }, "no window titled as the game")

    @Test
    fun `the game's window is found by its title, minimised too`() {
        val shown = game().handle
        SwingUtilities.invokeAndWait { standIn.extendedState = JFrame.ICONIFIED }
        Thread.sleep(SETTLE_MS)

        assertEquals(shown, game().handle)
    }

    @Test
    fun `the game's window opens the memory of the process that draws it`() {
        val written = Buffer(16).apply { write(0, ByteArray(16) { (it * 3).toByte() }, 0, 16) }

        GameMemory.of(game().handle).getOrThrow().use { memory ->
            assertEquals((0 until 16).map { (it * 3).toByte() }, memory.read(Pointer.nativeValue(written), 16)?.toList())
        }
    }

    private companion object {
        const val WIDTH = 640
        const val HEIGHT = 360
        const val SETTLE_MS = 500L
    }
}
