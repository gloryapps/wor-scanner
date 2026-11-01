package com.gloryapps.worscanner.scanner.client

import com.gloryapps.worscanner.scanner.senses.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** What the game's client says of itself: a unit's row by its id, every unit it holds, and the English it prints by key. */
interface Client {
    fun unit(id: Long): JsonObject?

    fun units(): List<Long>

    fun text(key: String): String?
}

/** The client as the player's PC holds it, read with the fields its running game gives its classes. */
class InstalledClient private constructor(private val units: GameTable, private val texts: GameTexts) : Client {
    override fun unit(id: Long): JsonObject? = units.row(id)

    override fun units(): List<Long> = units.keys()

    override fun text(key: String): String? = texts.text(key)

    companion object {
        /**
         * The client `memory` runs: its install in `install`, its Unity data in `data`, and in `saved` the
         * folder Unity gives it, where it keeps what it downloaded since, which it reads in place of the
         * install's: newer bundles, and its version's hotfixes, the last of each table.
         */
        suspend fun read(memory: Memory, install: File, data: File, saved: File): InstalledClient {
            val fields = classFields(memory, UNIT_ANCHOR)

            return withContext(Dispatchers.IO) {
                val game = File(saved, GAME)
                val version = File(install, MANIFEST).takeIf(File::exists)?.let { Json.parseToJsonElement(it.readText()).jsonObject["version"]?.jsonPrimitive?.content }
                val hotfixes = version?.let { File(game, "ParseRes/${it.split('.').takeLast(2).joinToString(".")}") }
                fun bundle(name: String) = File(game, "assets/Document/$name").takeIf(File::exists) ?: File(data, "StreamingAssets/Document/$name")
                fun table(name: String): ByteArray = hotfixes?.listFiles().orEmpty()
                    .mapNotNull { folder -> HOTFIX.find(folder.name)?.let { it.groupValues[1].toInt() to File(folder, "PackageOne/Document/$name.bytes") } }
                    .filter { (_, file) -> file.exists() }.maxByOrNull { (number, _) -> number }?.second?.readBytes()
                    ?: assetIn(bundle(DOCUMENT).readBytes(), name)
                fun texts(name: String) = bundle("$name.unity3d").takeIf(File::exists)?.let { assetIn(it.readBytes(), name) }

                InstalledClient(
                    GameTable(table(UNITS), fields),
                    GameTexts(*listOfNotNull(texts(ENGLISH) ?: error("the client holds no English"), texts(EXTRA)).toTypedArray()),
                )
            }
        }

        /** A field only the class of units declares. */
        private const val UNIT_ANCHOR = "m_HeroSkillLevelupList"
        private const val UNITS = "Unit"
        private const val DOCUMENT = "Document.unity3d"
        private const val ENGLISH = "AllLanguageEN"
        /* A hero added after a client was built has his name here, and none in the English. */
        private const val EXTRA = "ExLanguageEN_0"
        /** The game's own folder in the one Unity gives it. */
        private const val GAME = "dragon2019"
        /** What the game's launcher keeps beside it, its version among it. */
        private const val MANIFEST = "manifest.json"
        /** A hotfix's folder ends in its number, the highest read last. */
        private val HOTFIX = Regex("_(\\d+)$")
    }
}
