package com.gloryapps.worscanner.capture

import java.io.File

/**
 * An emulator whose Android holds a folder the PC sees too: identity, and where that folder is
 * mounted, the likeliest mount first. Both mount outside the sdcard, where no grant is needed;
 * BlueStacks moved its Shared Folder between versions, so its own mount `/mnt/windows` closes the list.
 */
enum class Emulator(val mounts: List<String>) {
    LD_PLAYER(listOf("/mnt/shared/Pictures", "/mnt/shared/Picture")),
    BLUE_STACKS(
        listOf(
            "/mnt/windows/BstSharedFolder",
            "/sdcard/windows/BstSharedFolder",
            "/sdcard/bstfolder/BstSharedFolder",
            "/mnt/windows",
        ),
    ),
}

/** The folder an emulator shares with the PC as this device has it, which is the shortest way from a scan to the lab. */
data class SharedFolder(val emulator: Emulator, val path: String)

/** Every shared folder this device has, the first open mount of each emulator: one on an emulator, none on a phone. */
fun sharedFolders(): List<SharedFolder> =
    Emulator.entries.mapNotNull { emulator -> emulator.mounts.firstOrNull(::writableFolder)?.let { SharedFolder(emulator, it) } }

private fun writableFolder(path: String): Boolean = File(path).let { it.isDirectory && it.canWrite() }
