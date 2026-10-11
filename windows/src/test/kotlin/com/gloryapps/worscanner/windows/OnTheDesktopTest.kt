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
import kotlin.test.assertTrue
import com.sun.jna.Memory as Buffer

/** A window on a real Windows desktop, listed as the desktop shows it and its process read. Run on a Windows runner by hand; skipped on any other system. */
class OnTheDesktopTest {
    private lateinit var standIn: JFrame

    @BeforeTest
    fun open() {
        assumeTrue("the desktop is Windows'", Desktop.present)
        SwingUtilities.invokeAndWait {
            standIn = JFrame(TITLE).apply {
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

    private fun listed(): TopWindow = assertNotNull(runBlocking { Desktop.windows() }.firstOrNull { it.title == TITLE }, "the stand-in is not listed")

    @Test
    fun `a window is listed with its title and class, and still shown once minimised`() {
        val shown = listed()
        SwingUtilities.invokeAndWait { standIn.extendedState = JFrame.ICONIFIED }
        Thread.sleep(SETTLE_MS)
        val minimised = listed()

        assertEquals("SunAwtFrame", shown.kind)
        assertEquals(shown.handle, minimised.handle)
        assertTrue(minimised.shown)
    }

    @Test
    fun `a window opens the memory of the process that draws it`() {
        val written = Buffer(16).apply { write(0, ByteArray(16) { (it * 3).toByte() }, 0, 16) }

        GameMemory.of(listed().handle).getOrThrow().use { memory ->
            assertEquals((0 until 16).map { (it * 3).toByte() }, memory.read(Pointer.nativeValue(written), 16)?.toList())
        }
    }

    private companion object {
        const val TITLE = "A stand-in for the game"
        const val WIDTH = 640
        const val HEIGHT = 360
        const val SETTLE_MS = 500L
    }
}
