package com.gloryapps.worscanner.scanner.simulator

import com.gloryapps.worscanner.scanner.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * The ways the Armory's simulator lets a comp be played, heroes by name, as its `GET /rules` answers: whom it gears as healers and who
 * inspires, each stance hero's stances, each other hero's Ultimate timings, the first where one left unpicked stays, and the ticks that say
 * where a hero stands. A dev build asks the simulator on the PC, so its Debug window offers what the Armory's pages offer.
 */
@Serializable
data class Rules(
    val healers: List<String> = emptyList(),
    val inspirers: List<String> = emptyList(),
    val stances: Map<String, List<String>> = emptyMap(),
    val ultimates: Map<String, List<String>> = emptyMap(),
    val ticks: Map<String, List<String>> = emptyMap(),
)

/** The rules of the simulator at `site`; null where none answers. */
suspend fun rulesAt(site: String): Rules? = withContext(Dispatchers.IO) {
    resultOf {
        val connection = (URL("$site/rules").openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        connection.inputStream.bufferedReader().use { rulesOf(it.readText()) }
    }.getOrNull()
}

/** The rules an answer holds, whatever else it says of the simulator's pages. */
internal fun rulesOf(answer: String): Rules = JSON.decodeFromString(Rules.serializer(), answer)

private val JSON = Json { ignoreUnknownKeys = true }

private const val TIMEOUT_MS = 3_000
