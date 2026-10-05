package com.gloryapps.worscanner.windows.game

import com.gloryapps.worscanner.scanner.text.Box
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameWindowTest {
    private fun window(title: String, width: Int = 1280, height: Int = 720, shown: Boolean = true, handle: Long = 1) =
        TopWindow(handle, title, shown, Box(0, 0, width, height))

    @Test
    fun `the game is the window titled as the game`() {
        val game = gameAmong(listOf(window("WoR Scanner", handle = 1), window("Watcher of Realms", handle = 2)))

        assertEquals(2, game?.handle)
    }

    @Test
    fun `a browser on the wiki is not the game, though its title names it`() {
        assertNull(gameAmong(listOf(window("Watcher of Realms Wiki | Fandom - Vivaldi", width = 1920, height = 1080))))
    }

    @Test
    fun `a game minimised or hidden is not found`() {
        assertNull(gameAmong(listOf(window("Watcher of Realms", shown = false))))
    }

    @Test
    fun `of two windows titled as the game, the larger is the one it draws in`() {
        val game = gameAmong(listOf(window("Watcher of Realms", 160, 90, handle = 1), window("Watcher of Realms", 1920, 1080, handle = 2)))

        assertEquals(2, game?.handle)
    }
}
