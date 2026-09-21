package com.gloryapps.worscanner.capture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EmulatorsTest {

    @Test
    fun `an emulator shares the mount this device has`() {
        assertEquals(
            SharedFolder(Emulator.LD_PLAYER, "/mnt/shared/Pictures"),
            Emulator.LD_PLAYER.sharedFolder(writable = { it == "/mnt/shared/Pictures" }),
        )
    }

    @Test
    fun `an older mount is shared when the current one is not there`() {
        assertEquals(
            SharedFolder(Emulator.BLUE_STACKS, "/sdcard/bstfolder/BstSharedFolder"),
            Emulator.BLUE_STACKS.sharedFolder(writable = { it == "/sdcard/bstfolder/BstSharedFolder" }),
        )
    }

    @Test
    fun `a mount the app cannot write to is no door out`() {
        assertNull(Emulator.LD_PLAYER.sharedFolder(writable = { false }))
    }

    @Test
    fun `a phone shares nothing`() {
        assertEquals(emptyList(), sharedFolders(writable = { false }))
    }

    @Test
    fun `the emulator running is the one whose folder is mounted`() {
        assertEquals(
            listOf(SharedFolder(Emulator.BLUE_STACKS, "/sdcard/windows/BstSharedFolder")),
            sharedFolders(writable = { it.startsWith("/sdcard") }),
        )
    }
}
