package com.gloryapps.worscanner.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** A version as `versionName` writes it, `0.2.0`, compared part by part. */
data class Version(private val parts: List<Int>) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        parts.zip(other.parts).map { (mine, theirs) -> mine.compareTo(theirs) }.firstOrNull { it != 0 } ?: parts.size.compareTo(other.parts.size)

    override fun toString() = parts.joinToString(".")

    companion object {
        /** The version a name like `0.2.0`, a tag like `v0.2.0` or a dev build's `0.2.0-dev` stands for; null where it is none. */
        fun of(name: String): Version? = Version(name.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: return null })
    }
}

/** A release the app can update to: its version, its APK, and its page for when the APK cannot be had. */
data class Release(val version: Version, val apk: String, val page: String)

/** Where the app's releases are published. */
interface Releases {
    /** The newest release; null where it could not be learnt. */
    suspend fun latest(): Release?

    /** The release's APK on disk, for the system's installer; null where it could not be had. */
    suspend fun download(release: Release): File?
}

/** Where updating the app stands. */
sealed interface Update {
    /** No newer release is known: none is out, or the check did not answer. */
    data object None : Update

    data class Available(val release: Release) : Update

    data class Downloading(val release: Release) : Update

    /** The APK could not be had: the release's page is offered instead. */
    data class Failed(val release: Release) : Update
}

/** Whether a newer release than the running one is out, and its APK fetched for the system's installer. */
class Updates(private val releases: Releases, private val running: Version?) {
    private val _state = MutableStateFlow<Update>(Update.None)
    val state: StateFlow<Update> = _state.asStateFlow()

    suspend fun check() {
        val latest = releases.latest() ?: return
        if (running != null && latest.version > running && _state.value == Update.None) _state.value = Update.Available(latest)
    }

    /** The newer release's APK; null where there is none to fetch or it could not be had, its page offered from then on. */
    suspend fun download(): File? {
        val release = (_state.value as? Update.Available)?.release ?: return null
        _state.value = Update.Downloading(release)
        val apk = releases.download(release)
        /* Left available after a download, so an install the player backed out of can be started again. */
        _state.value = if (apk != null) Update.Available(release) else Update.Failed(release)

        return apk
    }
}
