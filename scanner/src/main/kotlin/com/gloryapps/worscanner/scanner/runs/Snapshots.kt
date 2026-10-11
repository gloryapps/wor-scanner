package com.gloryapps.worscanner.scanner.runs

import com.gloryapps.worscanner.scanner.lua.LuaHeap
import com.gloryapps.worscanner.scanner.lua.LuaValue
import com.gloryapps.worscanner.scanner.lua.name
import com.gloryapps.worscanner.scanner.senses.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.time.LocalDateTime
import java.util.zip.GZIPOutputStream

/** A snapshot's first line: when it was taken, the game's window then, how many runs of memory and bytes were swept, and how many tables were found in them. */
@Serializable
data class Snapshot(val takenAt: String, val width: Int, val height: Int, val spans: Int, val bytes: Long, val tables: Int)

/** Every table in the game's memory at one moment, for a diagnosis: a gzipped file of JSON lines in `snapshots`, named by the stamp it was taken at. */
class Snapshots(private val root: File) {
    /** Takes one of `memory` beside a game's window `width` by `height`: the [Snapshot], then each table on a line of its own, lowest address first. */
    suspend fun take(memory: Memory, width: Int, height: Int): File = withContext(Dispatchers.IO) {
        val takenAt = LocalDateTime.now().format(STAMP)
        val heap = LuaHeap(memory)
        val spans = memory.spans()
        val tables = heap.tables()
        val file = File(root, "snapshots/$takenAt.jsonl.gz").apply { parentFile.mkdirs() }

        GZIPOutputStream(file.outputStream()).bufferedWriter().use { out ->
            out.appendLine(Json.encodeToString(Snapshot.serializer(), Snapshot(takenAt, width, height, spans.size, spans.sumOf { it.size }, tables.size)))
            /* A batch at a time, read side by side and written in order, so no more than a batch is held. */
            tables.asSequence().chunked(BATCH).forEach { batch ->
                batch.map { async(Dispatchers.Default) { heap.line(it) } }.awaitAll().filterNotNull().forEach { out.appendLine(it) }
            }
        }

        file
    }
}

/** A table as one line: where it lies, its array part's size, and each part's first values, the array's in order and the hash part's by key. */
private fun LuaHeap.line(address: Long): String? = table(address, MOST)?.let { table ->
    JsonObject(
        mapOf(
            "at" to JsonPrimitive(address),
            "length" to JsonPrimitive(table.length),
            "array" to JsonArray(table.array.map { it?.let(::json) ?: JsonNull }),
            "fields" to JsonObject(table.hash.entries.associate { (key, value) -> key.name to json(value) }),
        ),
    ).toString()
}

/** A value as a line holds it: data as itself, a string cut short, a table as where it lies and anything else as its tag. */
private fun json(value: LuaValue): JsonElement = when (value) {
    is LuaValue.Bool -> JsonPrimitive(value.value)
    is LuaValue.Integer -> JsonPrimitive(value.value)
    is LuaValue.Real -> if (value.value.isFinite()) JsonPrimitive(value.value) else JsonNull
    is LuaValue.Text -> JsonPrimitive(value.value.take(MOST_CHARACTERS))
    is LuaValue.Table -> JsonObject(mapOf("table" to JsonPrimitive(value.address)))
    is LuaValue.Other -> JsonObject(mapOf("other" to JsonPrimitive(value.tag)))
}

private const val MOST = 500
private const val MOST_CHARACTERS = 80
private const val BATCH = 1024
