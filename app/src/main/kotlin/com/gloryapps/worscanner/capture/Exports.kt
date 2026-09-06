package com.gloryapps.worscanner.capture

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File

/** How a reading leaves the device: into the public Downloads folder, or handed to another app. */
class Exports(private val context: Context) {
    /** True where saving to Downloads needs the storage permission first, which is Android 9 and below. */
    val downloadsNeedPermission: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun toDownloads(files: List<File>): List<String> = files.map { file ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) throughMediaStore(file) else ontoDisk(file, publicFolder(Environment.DIRECTORY_DOWNLOADS))
    }

    /**
     * The Pictures folder an emulator shares with the PC: LDPlayer mounts it at `/mnt/shared/Pictures`
     * and shows it under the Windows Documents folder. A device without that mount has no such door,
     * and says so rather than hiding a JSON among the photos.
     */
    fun toPictures(files: List<File>): List<String> {
        val shared = File(SHARED_PICTURES)
        check(shared.isDirectory && shared.canWrite()) { "no shared Pictures folder on this device" }
        val folder = File(shared, FOLDER).apply { mkdirs() }

        return files.map { ontoDisk(it, folder) }
    }

    fun shareIntent(files: List<File>): Intent {
        val uris = ArrayList<Uri>(files.map { FileProvider.getUriForFile(context, "${context.packageName}.files", it) })

        return Intent.createChooser(
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("*/*")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            null,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun throughMediaStore(file: File): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mimeOf(file))
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
        }
        val resolver = context.contentResolver
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) { "Downloads refused ${file.name}" }
        resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }

        return "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/${file.name}"
    }

    @Suppress("DEPRECATION")
    private fun publicFolder(directory: String): File =
        File(Environment.getExternalStoragePublicDirectory(directory), FOLDER).apply { mkdirs() }

    private fun ontoDisk(file: File, folder: File): String {
        val copy = File(folder, file.name)
        file.copyTo(copy, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(copy.path), null, null)

        return copy.path
    }

    private fun mimeOf(file: File) = if (file.extension == "png") "image/png" else "application/json"

    private companion object {
        const val FOLDER = "WoR Scanner"
        const val SHARED_PICTURES = "/mnt/shared/Pictures"
    }
}
