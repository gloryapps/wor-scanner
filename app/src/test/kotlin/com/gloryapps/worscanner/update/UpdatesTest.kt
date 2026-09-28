package com.gloryapps.worscanner.update

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdatesTest {
    /** The API's answer for v0.1.2 as it came on 2026-09-28, cut to the fields around those read. */
    private val latest = """
        {
          "tag_name": "v0.1.2",
          "name": "v0.1.2",
          "html_url": "https://github.com/gloryapps/wor-scanner/releases/tag/v0.1.2",
          "assets": [
            {"name": "mapping-0.1.2.txt.gz", "browser_download_url": "https://github.com/gloryapps/wor-scanner/releases/download/v0.1.2/mapping-0.1.2.txt.gz"},
            {"name": "wor-scanner.apk", "browser_download_url": "https://github.com/gloryapps/wor-scanner/releases/download/v0.1.2/wor-scanner.apk", "size": 1980672}
          ]
        }
    """.trimIndent()

    private val release = Release(Version.of("0.3.0")!!, "https://example.test/wor-scanner.apk", "https://example.test/v0.3.0")

    /** Releases that answer with what they are given, and count the downloads asked of them. */
    private class Published(private val latest: Release?, private val apk: File?) : Releases {
        var downloads = 0

        override suspend fun latest(): Release? = latest

        override suspend fun download(release: Release): File? = apk.also { downloads++ }
    }

    @Test
    fun `a version is compared part by part, a tag's v and a dev build's suffix dropped`() {
        assertTrue(Version.of("0.10.0")!! > Version.of("0.9.3")!!)
        assertTrue(Version.of("1.0.0")!! > Version.of("0.12.4")!!)
        assertEquals(Version.of("0.2.0"), Version.of("v0.2.0"))
        assertEquals(Version.of("0.2.0"), Version.of("0.2.0-dev"))
        assertNull(Version.of("latest"))
    }

    @Test
    fun `the API's answer names the release's version, its APK and its page`() {
        val read = releaseIn(latest)

        assertEquals(Version.of("0.1.2"), read?.version)
        assertEquals("https://github.com/gloryapps/wor-scanner/releases/download/v0.1.2/wor-scanner.apk", read?.apk)
        assertEquals("https://github.com/gloryapps/wor-scanner/releases/tag/v0.1.2", read?.page)
    }

    @Test
    fun `an answer without the APK, with a tag that is no version, or that is no answer at all is no release`() {
        assertNull(releaseIn(latest.replace("\"wor-scanner.apk\"", "\"other.apk\"")))
        assertNull(releaseIn(latest.replace("\"v0.1.2\"", "\"nightly\"")))
        assertNull(releaseIn("""{"message": "API rate limit exceeded"}"""))
        assertNull(releaseIn("<html>"))
    }

    @Test
    fun `a newer release is offered, the running one and an older one are not`() = runBlocking {
        suspend fun checked(running: String) = Updates(Published(release, null), Version.of(running)).apply { check() }.state.value

        assertEquals(Update.Available(release), checked("0.2.0"))
        assertEquals(Update.None, checked("0.3.0"))
        assertEquals(Update.None, checked("0.4.0-dev"))
    }

    @Test
    fun `a check that learnt nothing offers nothing`() = runBlocking {
        val updates = Updates(Published(null, null), Version.of("0.2.0"))

        updates.check()

        assertEquals(Update.None, updates.state.value)
    }

    @Test
    fun `a downloaded APK leaves the update offered, so a backed-out install can be started again`() = runBlocking {
        val apk = File("wor-scanner.apk")
        val updates = Updates(Published(release, apk), Version.of("0.2.0")).apply { check() }

        assertEquals(apk, updates.download())
        assertEquals(Update.Available(release), updates.state.value)
    }

    @Test
    fun `an APK that could not be had offers the release's page instead`() = runBlocking {
        val published = Published(release, null)
        val updates = Updates(published, Version.of("0.2.0")).apply { check() }

        assertNull(updates.download())
        assertEquals(Update.Failed(release), updates.state.value)
        assertNull(updates.download())
        assertEquals(1, published.downloads)
    }
}
