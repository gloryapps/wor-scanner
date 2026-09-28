package com.gloryapps.worscanner.update

import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** The releases the workflow publishes on GitHub (`bootstrap/ci.md`), learnt through its API. */
class GitHubReleases(private val folder: File) : Releases {
    override suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        resultOf { open(LATEST).apply { setRequestProperty("Accept", "application/vnd.github+json") }.inputStream.bufferedReader().use { it.readText() } }
            .getOrNull()
            ?.let(::releaseIn)
    }

    override suspend fun download(release: Release): File? = withContext(Dispatchers.IO) {
        resultOf {
            val apk = File(folder.apply { mkdirs() }, APK)
            open(release.apk).inputStream.use { input -> apk.outputStream().use { input.copyTo(it) } }
            apk.takeIf { it.length() > 0 }
        }.getOrNull()
    }

    /* A GitHub download redirects to its storage host, which the connection follows, both being https. */
    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = TIMEOUT
        readTimeout = TIMEOUT
        setRequestProperty("User-Agent", "wor-scanner")
    }
}

/** The release the API's answer describes; null where it names no version or no APK, whatever else it holds. */
internal fun releaseIn(answer: String): Release? {
    val latest = resultOf { JSON.decodeFromString(LatestRelease.serializer(), answer) }.getOrNull() ?: return null
    val version = latest.tag?.let(Version::of) ?: return null
    val apk = latest.assets.orEmpty().firstOrNull { it.name == APK }?.url ?: return null

    return Release(version, apk, latest.page ?: LATEST_PAGE)
}

/** Only what is read of the API's answer, every field of which may be missing. */
@Serializable
private class LatestRelease(
    @SerialName("tag_name") val tag: String? = null,
    @SerialName("html_url") val page: String? = null,
    @SerialName("assets") val assets: List<Asset>? = null,
)

@Serializable
private class Asset(
    @SerialName("name") val name: String? = null,
    @SerialName("browser_download_url") val url: String? = null,
)

private const val REPOSITORY = "gloryapps/wor-scanner"
private const val LATEST = "https://api.github.com/repos/$REPOSITORY/releases/latest"
private const val LATEST_PAGE = "https://github.com/$REPOSITORY/releases/latest"
/** The APK every release holds, named without its version. */
private const val APK = "wor-scanner.apk"
private const val TIMEOUT = 15_000
private val JSON = Json { ignoreUnknownKeys = true }
