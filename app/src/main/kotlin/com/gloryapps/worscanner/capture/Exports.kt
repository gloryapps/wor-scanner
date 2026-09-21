package com.gloryapps.worscanner.capture

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** One file on its way out and the name it leaves under, which is the caller's to say, never this file's. */
data class Outbound(val file: File, val name: String)

/** How a reading leaves the device: into a folder an emulator shares with the PC, or handed to another app. */
class Exports(private val context: Context) {
    /** The shared folders this device has, asked once a reading is on its way out rather than kept from start-up. */
    fun shared(): List<SharedFolder> = sharedFolders()

    /** Into the folder an emulator shares with the PC, under this app's own name so the PC side stays tidy. */
    fun toShared(shared: SharedFolder, files: List<Outbound>): List<String> {
        val folder = File(shared.path, FOLDER).apply { mkdirs() }
        check(folder.isDirectory) { "${shared.path} is not writable on this device" }

        return files.map { ontoDisk(it, folder) }
    }

    /** The other app is shown the name the file goes out under, which is why each one is staged in the cache first. */
    fun shareIntent(files: List<Outbound>): Intent {
        val staged = File(context.cacheDir, OUTGOING).apply { deleteRecursively(); mkdirs() }
        val uris = ArrayList<Uri>(files.map { FileProvider.getUriForFile(context, "${context.packageName}.files", copied(it, staged)) })

        return Intent.createChooser(
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("*/*")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            null,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun ontoDisk(out: Outbound, folder: File): String {
        val copy = copied(out, folder)
        MediaScannerConnection.scanFile(context, arrayOf(copy.path), null, null)

        return copy.path
    }

    private fun copied(out: Outbound, folder: File): File = out.file.copyTo(File(folder, out.name), overwrite = true)

    private companion object {
        const val FOLDER = "WoR Scanner"
        const val OUTGOING = "outgoing"
    }
}
