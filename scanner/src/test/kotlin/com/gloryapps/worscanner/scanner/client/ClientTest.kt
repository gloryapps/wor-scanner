package com.gloryapps.worscanner.scanner.client

import com.gloryapps.worscanner.scanner.senses.Memory
import com.gloryapps.worscanner.scanner.senses.Span
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** A 1.6.46 client as a PC holds it: its install, and beside it the folder Unity gives it, with what it downloaded since. */
class ClientTest {
    private val root: File = createTempDirectory().toFile()
    private val install = File(root, "install")
    private val data = File(install, "WoR_Data")
    private val saved = File(root, "LocalLow")

    /** A game whose memory holds no class, so a unit's row is read as no fields. */
    private val memory = object : Memory {
        override fun spans(): List<Span> = emptyList()

        override fun read(address: Long, size: Int): ByteArray? = null
    }

    private fun lay(path: String, bytes: ByteArray) = File(root, path).apply { parentFile.mkdirs(); writeBytes(bytes) }

    /** A table of units, each row holding no field. */
    private fun units(vararg keys: Int) = mufTable(emptyList(), keys.associateWith { ByteArray(0) })

    init {
        lay("install/manifest.json", """{"version":"1.6.46"}""".encodeToByteArray())
        lay("install/WoR_Data/StreamingAssets/Document/AllLanguageEN.unity3d", bundleOf("AllLanguageEN" to textTable(emptyMap())))
        lay("install/WoR_Data/StreamingAssets/Document/Document.unity3d", bundleOf("Unit" to units(2001)))
    }

    @Test
    fun `the units are read from the last hotfix of the client's version that holds them`() = runTest {
        lay("LocalLow/dragon2019/ParseRes/6.46/hotfix_9/PackageOne/Document/Unit.bytes", units(2001, 2901))
        lay("LocalLow/dragon2019/ParseRes/6.46/hotfix_10/PackageOne/Document/Unit.bytes", units(2001, 2901, 2903))
        File(saved, "dragon2019/ParseRes/6.46/hotfix_11").mkdirs()
        lay("LocalLow/dragon2019/ParseRes/6.45/hotfix_12/PackageOne/Document/Unit.bytes", units(2001))

        assertEquals(setOf(2001L, 2901L, 2903L), InstalledClient.read(memory, install, data, saved).units().toSet())
    }

    @Test
    fun `a table its class does not read is refused while the client is read`() = runTest {
        lay("install/WoR_Data/StreamingAssets/Document/Document.unity3d", bundleOf("Unit" to mufTable(emptyList(), mapOf(2903 to numbers(1)))))

        assertFailsWith<IllegalStateException> { InstalledClient.read(memory, install, data, saved) }
    }
}
