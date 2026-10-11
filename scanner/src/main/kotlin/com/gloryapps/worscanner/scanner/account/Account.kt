package com.gloryapps.worscanner.scanner.account

import com.gloryapps.worscanner.scanner.lua.LuaHeap
import com.gloryapps.worscanner.scanner.lua.LuaValue
import com.gloryapps.worscanner.scanner.senses.Memory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.TimeSource

/** The account as the running game holds it, each entry in the game's own fields: the file the lab imports, told from a scan by its `kind`. */
@Serializable
data class Account(
    @EncodeDefault val kind: String = "account",
    val startedAt: String,
    val heroes: List<JsonElement>,
    val gear: List<JsonElement>,
    val artifacts: List<JsonElement>,
    /** How many entries every table holding each list had: the largest is the one read, the others copies the game let go. */
    val found: Map<Holding, List<Int>>,
    val seconds: Double,
)

/** Where the game keeps a list: under `list`, in the table holding `anchor`, a field only that table has; `shared` are fields holding the game's row for every entry of a kind. */
@Serializable
enum class Holding(val anchor: String, val list: String, val shared: Set<String>) {
    @SerialName("heroes") HEROES(anchor = "m_CharactorDatas", list = "m_CharactorDatas", shared = setOf("config")),
    @SerialName("gear") GEAR(anchor = "suitId2EquipIdExt", list = "equips", shared = setOf("vConfig", "vItemConfig", "vSuitConfig")),
    @SerialName("artifacts") ARTIFACTS(anchor = "m_vArtifacts", list = "m_vArtifacts", shared = setOf("m_AfCfg", "m_ItemCfg")),
}

/** Reads every list the game holds, from whichever of its holders holds the most; `startedAt` is the stamp the read is kept under. */
suspend fun readAccount(memory: Memory, startedAt: String): Account = withContext(Dispatchers.Default) {
    val started = TimeSource.Monotonic.markNow()
    val heap = LuaHeap(memory)
    val holders = heap.holders(Holding.entries.map { it.anchor }.toSet())
    val lists = Holding.entries.associateWith { holding -> holders[holding.anchor].orEmpty().map { heap.entries(it, holding.list) } }
    val read = lists.mapValues { (holding, candidates) -> candidates.maxByOrNull { it.size }.orEmpty().mapNotNull { heap.entry(it, holding) } }

    Account(
        startedAt = startedAt,
        heroes = read.getValue(Holding.HEROES),
        gear = read.getValue(Holding.GEAR),
        artifacts = read.getValue(Holding.ARTIFACTS),
        found = lists.mapValues { (_, candidates) -> candidates.map { it.size }.sortedDescending() },
        seconds = started.elapsedNow().inWholeMilliseconds / 1000.0,
    )
}

/** The tables the field `list` of the table at `holder` holds. */
private fun LuaHeap.entries(holder: Long, list: String): List<LuaValue.Table> =
    (field(holder, list) as? LuaValue.Table)?.let { fields(it.address) }?.values?.filterIsInstance<LuaValue.Table>().orEmpty()

/** An entry as JSON without its `shared` fields, read to an attribute in a piece's attribute list. */
private fun LuaHeap.entry(entry: LuaValue.Table, holding: Holding): JsonElement? =
    fields(entry.address)?.filterKeys { (it as? LuaValue.Text)?.value !in holding.shared }?.let { json(it, depth = 2) }

/** A value as JSON, its tables read `depth` deep; null for what is not data. */
private fun LuaHeap.json(value: LuaValue, depth: Int): JsonElement? = when (value) {
    is LuaValue.Bool -> JsonPrimitive(value.value)
    is LuaValue.Integer -> JsonPrimitive(value.value)
    is LuaValue.Real -> value.value.takeIf { it.isFinite() }?.let(::JsonPrimitive)
    is LuaValue.Text -> JsonPrimitive(value.value)
    is LuaValue.Other -> null
    is LuaValue.Table -> if (depth == 0) null else fields(value.address)?.let { json(it, depth - 1) }
}

/** A table keyed 1 to n is an array, in that order; any other an object. */
private fun LuaHeap.json(fields: Map<LuaValue, LuaValue>, depth: Int): JsonElement {
    val values = fields.mapNotNull { (key, field) -> json(field, depth)?.let { key to it } }
    val indices = values.map { (key, _) -> (key as? LuaValue.Integer)?.value }

    return if (indices.toSet() == (1L..values.size).toSet()) {
        JsonArray(values.sortedBy { (key, _) -> (key as LuaValue.Integer).value }.map { it.second })
    } else {
        JsonObject(values.associate { (key, field) -> ((key as? LuaValue.Text)?.value ?: (key as LuaValue.Integer).value.toString()) to field })
    }
}
