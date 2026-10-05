package com.gloryapps.worscanner.windows.capture

import com.gloryapps.worscanner.scanner.senses.Frame
import com.gloryapps.worscanner.scanner.senses.Screen
import com.gloryapps.worscanner.scanner.text.Box
import com.gloryapps.worscanner.windows.game.Desktop
import com.gloryapps.worscanner.windows.game.Win32Thread
import com.sun.jna.Memory
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinGDI
import kotlinx.coroutines.withContext

/**
 * The game's window copied off the desktop as it is composited, which is where a Unity frame can be
 * read. The copy waits for the game to be in front, as the hand does, so another window is never read
 * in its place. The cursor is never in the copy.
 */
internal class DesktopScreen(private val handle: Long) : Screen {
    override suspend fun capture(): Frame {
        Desktop.awaitFront(handle)

        return withContext(Win32Thread) { copyOf(checkNotNull(Desktop.clientOf(handle)) { "the game's window is closed or minimised" }) }
    }

    private fun copyOf(area: Box): PixelFrame {
        val gdi = GDI32.INSTANCE
        val desktop = User32.INSTANCE.GetDC(null)
        val copy = gdi.CreateCompatibleDC(desktop)
        val bitmap = gdi.CreateCompatibleBitmap(desktop, area.width, area.height)
        try {
            val held = gdi.SelectObject(copy, bitmap)
            check(gdi.BitBlt(copy, 0, 0, area.width, area.height, desktop, area.left, area.top, GDI32.SRCCOPY)) { "the desktop could not be copied" }
            gdi.SelectObject(copy, held)
            val pixels = Memory(area.width.toLong() * area.height * 4)
            check(gdi.GetDIBits(copy, bitmap, 0, area.height, pixels, topDown(area), WinGDI.DIB_RGB_COLORS) == area.height) { "the copy could not be read" }

            return PixelFrame.ofBgra(pixels.getByteBuffer(0, pixels.size()), area.width, area.height)
        } finally {
            gdi.DeleteObject(bitmap)
            gdi.DeleteDC(copy)
            User32.INSTANCE.ReleaseDC(null, desktop)
        }
    }

    /** 32 bits a pixel, rows from the top: a negative height is how a DIB says so. */
    private fun topDown(area: Box) = WinGDI.BITMAPINFO().apply {
        bmiHeader.biWidth = area.width
        bmiHeader.biHeight = -area.height
        bmiHeader.biPlanes = 1
        bmiHeader.biBitCount = 32
        bmiHeader.biCompression = WinGDI.BI_RGB
    }
}
