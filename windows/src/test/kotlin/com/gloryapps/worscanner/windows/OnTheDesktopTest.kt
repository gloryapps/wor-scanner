package com.gloryapps.worscanner.windows

import com.gloryapps.worscanner.scanner.runs.write
import com.gloryapps.worscanner.windows.capture.DesktopScreen
import com.gloryapps.worscanner.windows.capture.ImagePictures
import com.gloryapps.worscanner.windows.game.Desktop
import com.gloryapps.worscanner.windows.game.TopWindow
import com.gloryapps.worscanner.windows.game.UserCalls
import com.gloryapps.worscanner.windows.hand.Button
import com.gloryapps.worscanner.windows.hand.MouseHand
import com.gloryapps.worscanner.windows.hand.press
import com.gloryapps.worscanner.windows.ocr.WindowsOcr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.roundToInt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The senses against a real Windows desktop: a window titled as the game is found, copied, read and
 * clicked. Run on a Windows runner by hand; skipped on any other system.
 */
class OnTheDesktopTest {
    /** What the stand-in was given by the mouse, in its own pixels. */
    private data class Pressed(val what: String, val x: Int, val y: Int)

    /** Titled as the game, a white face with a known name on it, keeping what the mouse does to it. */
    private class StandIn : AutoCloseable {
        val events = LinkedBlockingQueue<Pressed>()
        private lateinit var frame: JFrame

        /** Device pixels per Swing pixel: the frame the senses copy counts device pixels. */
        val scale: Double get() = frame.graphicsConfiguration.defaultTransform.scaleX

        init {
            SwingUtilities.invokeAndWait {
                val face = object : JPanel() {
                    override fun paintComponent(g: Graphics) {
                        g.color = Color.WHITE
                        g.fillRect(0, 0, width, height)
                        g.color = Color.BLACK
                        g.font = Font(Font.SANS_SERIF, Font.BOLD, 36)
                        g.drawString(NAME, 40, 120)
                    }
                }.apply {
                    preferredSize = Dimension(WIDTH, HEIGHT)
                    val listener = object : MouseAdapter() {
                        override fun mousePressed(e: MouseEvent) { events += Pressed("press", e.x, e.y) }
                        override fun mouseDragged(e: MouseEvent) { events += Pressed("drag", e.x, e.y) }
                        override fun mouseReleased(e: MouseEvent) { events += Pressed("release", e.x, e.y) }
                    }
                    addMouseListener(listener)
                    addMouseMotionListener(listener)
                }
                frame = JFrame("Watcher of Realms").apply {
                    contentPane = face
                    pack()
                    setLocation(80, 80)
                    isAlwaysOnTop = true
                    isVisible = true
                }
            }
            Thread.sleep(SETTLE_MS)
        }

        /** A click on its title, which brings it in front the way a player's would and leaves nothing on its face. */
        fun bringInFront() = frame.clickTitle()

        fun next(): Pressed? = events.poll(2, TimeUnit.SECONDS)

        override fun close() = SwingUtilities.invokeAndWait { frame.dispose() }
    }

    private lateinit var standIn: StandIn


    @BeforeTest
    fun open() {
        assumeTrue("the desktop is Windows'", Desktop.present)
        standIn = StandIn()
    }

    @AfterTest
    fun close() {
        if (::standIn.isInitialized) standIn.close()
    }

    private fun game(): TopWindow = assertNotNull(runBlocking { Desktop.game() }, "no window titled as the game")

    private fun pixels(swing: Int) = (swing * standIn.scale).roundToInt()

    @Test
    fun `the game's window is found by its title, as large as the face it draws`() {
        val client = game().client

        assertEquals(pixels(WIDTH) to pixels(HEIGHT), client.width to client.height)
    }

    @Test
    fun `the copy of the game's window shows what it draws`() {
        val frame = runBlocking { DesktopScreen(game().handle).capture() }
        File("build/desktop-test").apply { mkdirs() }.let { ImagePictures().write(frame, File(it, "frame.png")) }

        assertEquals(pixels(WIDTH) to pixels(HEIGHT), frame.width to frame.height)
        assertEquals(255, frame.colourAt(pixels(10), pixels(10)).paleness)
        assertTrue((pixels(40) until pixels(400)).any { x -> frame.colourAt(x, pixels(110)).paleness < 64 }, "nothing dark where the name is drawn")
    }

    @Test
    fun `Windows' OCR reads the name the window draws`() {
        val lines = runBlocking { WindowsOcr().read(DesktopScreen(game().handle).capture()) }

        assertTrue(lines.any { "Leonidas" in it.text }, "read ${lines.map { it.text }}")
    }

    @Test
    fun `a tap presses and lets go where the frame says, never moving off it`() {
        standIn.bringInFront()

        runBlocking { MouseHand(game().handle).tap(pixels(100), pixels(200)) }

        val events = generateSequence { standIn.next() }.toList()
        assertEquals(Pressed("press", 100, 200), events.first())
        assertEquals(Pressed("release", 100, 200), events.last())
        assertTrue(events.all { it.x == 100 && it.y == 200 }, "the tap moved: $events")
    }

    @Test
    fun `a drag presses, moves and lets go where it ends`() {
        standIn.bringInFront()

        runBlocking { MouseHand(game().handle).drag(pixels(100), pixels(250), pixels(400), pixels(250), 300) }

        val events = generateSequence { standIn.next() }.toList()
        assertEquals(Pressed("press", 100, 250), events.first())
        assertTrue(events.any { it.what == "drag" }, "no move while pressed: $events")
        assertEquals(Pressed("release", 400, 250), events.last())
    }

    @Test
    fun `with another window in front the hand waits, and taps once the game is back`() = runBlocking {
        val handle = game().handle
        val other = JFrame("Something else")
        SwingUtilities.invokeAndWait {
            other.setSize(500, 300)
            other.setLocation(760, 80)
            other.isVisible = true
        }
        try {
            other.clickTitle()

            val tap = async(Dispatchers.Default) { MouseHand(handle).tap(pixels(100), pixels(200)) }

            assertNull(standIn.next(), "the hand tapped with another window in front")
            standIn.bringInFront()
            tap.await()
            assertEquals(Pressed("press", 100, 200), standIn.next())
        } finally {
            SwingUtilities.invokeAndWait { other.dispose() }
        }
    }

    private companion object {
        const val NAME = "Spear of Leonidas"
        const val WIDTH = 640
        const val HEIGHT = 360
        const val SETTLE_MS = 500L
    }
}

/** A click on the window's title bar, where Windows brings it in front. */
private fun JFrame.clickTitle() {
    val scale = graphicsConfiguration.defaultTransform.scaleX
    val at = locationOnScreen
    UserCalls.user.SetCursorPos(((at.x + width / 2) * scale).roundToInt().toLong(), ((at.y + insets.top / 2) * scale).roundToInt().toLong())
    press(Button.DOWN)
    press(Button.UP)
    Thread.sleep(CLICK_SETTLE_MS)
}

private const val CLICK_SETTLE_MS = 500L
