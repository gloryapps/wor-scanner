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
 * inspires, each stance hero's stances, each other hero's Ultimate timings, the first where one left unpicked stays, the ticks that say
 * where a hero stands, and each Guild Boss difficulty by its number. A dev build asks the simulator on the PC, so its Debug window offers
 * what the Armory's pages offer.
 */
@Serializable
data class Rules(
    val healers: List<String> = emptyList(),
    val inspirers: List<String> = emptyList(),
    val stances: Map<String, List<String>> = emptyMap(),
    val ultimates: Map<String, List<String>> = emptyMap(),
    val ticks: Map<String, List<String>> = emptyMap(),
    val difficulties: Map<String, Difficulty> = emptyMap(),
) {
    /** The map the stage is fought on, none where the simulator holds none. */
    fun gridOf(stageId: Long): Grid? = difficulties.values.firstOrNull { it.stageId == stageId }?.grid
}

/** A Guild Boss difficulty: its stage, and the map heroes are put on. */
@Serializable
data class Difficulty(val stageId: Long? = null, val grid: Grid? = null)

/** A stage's map, cells as the simulator numbers them, `[x, y]` at their centres: where ground heroes go, where the others go, and the boss. */
@Serializable
data class Grid(val melee: List<List<Double>>, val ranged: List<List<Double>>, val boss: List<Double>)

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
