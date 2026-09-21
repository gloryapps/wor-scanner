package com.gloryapps.worscanner.capture

import java.io.File

/**
 * An emulator whose Android holds a folder the PC sees too: identity, and where that folder is
 * mounted, the current mount first. LDPlayer mounts it outside the sdcard, where the app writes with
 * no grant at all; BlueStacks puts its Shared Folder inside the sdcard, which needs files access.
 */
enum class Emulator(val mounts: List<String>) {
    LD_PLAYER(listOf("/mnt/shared/Pictures", "/mnt/shared/Picture")),
    BLUE_STACKS(listOf("/sdcard/windows/BstSharedFolder", "/sdcard/bstfolder/BstSharedFolder")),
}

/** The folder an emulator shares with the PC as this device has it, which is the shortest way from a scan to the lab. */
data class SharedFolder(val emulator: Emulator, val path: String)

/** Where this emulator shares here, or nothing when this is another emulator, or a phone, or the mount is closed to the app. */
fun Emulator.sharedFolder(writable: (String) -> Boolean = ::writableFolder): SharedFolder? =
    mounts.firstOrNull(writable)?.let { SharedFolder(this, it) }

/** Every shared folder this device has: one on an emulator, none on a phone. */
fun sharedFolders(writable: (String) -> Boolean = ::writableFolder): List<SharedFolder> =
    Emulator.entries.mapNotNull { it.sharedFolder(writable) }

private fun writableFolder(path: String): Boolean = File(path).let { it.isDirectory && it.canWrite() }
