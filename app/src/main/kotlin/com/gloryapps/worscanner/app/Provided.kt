package com.gloryapps.worscanner.app

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** A file of the app's cache as the manifest's file provider hands it to another app. */
fun Context.provided(file: File): Uri = FileProvider.getUriForFile(this, "$packageName.files", file)
